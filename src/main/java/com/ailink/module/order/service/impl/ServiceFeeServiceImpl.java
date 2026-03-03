package com.ailink.module.order.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.enums.OrderStatus;
import com.ailink.common.enums.PlatformPaymentStatus;
import com.ailink.common.enums.ServiceFeeStatus;
import com.ailink.common.exception.BizException;
import com.ailink.config.WechatPayProperties;
import com.ailink.module.order.entity.Order;
import com.ailink.module.order.entity.PlatformPayment;
import com.ailink.module.order.mapper.OrderMapper;
import com.ailink.module.order.mapper.PlatformPaymentMapper;
import com.ailink.module.order.payment.WechatNativeOrderResponse;
import com.ailink.module.order.payment.WechatNativePayService;
import com.ailink.module.order.payment.WechatNotifyTransaction;
import com.ailink.module.order.service.OrderService;
import com.ailink.module.order.service.ServiceFeeService;
import com.ailink.module.order.vo.ServiceFeePayVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class ServiceFeeServiceImpl implements ServiceFeeService {

    private static final Logger log = LoggerFactory.getLogger(ServiceFeeServiceImpl.class);

    private final OrderMapper orderMapper;
    private final PlatformPaymentMapper platformPaymentMapper;
    private final OrderService orderService;
    private final WechatNativePayService wechatNativePayService;
    private final ServiceFeePaymentTxService paymentTxService;
    private final WechatPayProperties wechatPayProperties;

    public ServiceFeeServiceImpl(OrderMapper orderMapper,
                                 PlatformPaymentMapper platformPaymentMapper,
                                 OrderService orderService,
                                 WechatNativePayService wechatNativePayService,
                                 ServiceFeePaymentTxService paymentTxService,
                                 WechatPayProperties wechatPayProperties) {
        this.orderMapper = orderMapper;
        this.platformPaymentMapper = platformPaymentMapper;
        this.orderService = orderService;
        this.wechatNativePayService = wechatNativePayService;
        this.paymentTxService = paymentTxService;
        this.wechatPayProperties = wechatPayProperties;
    }

    @Override
    public ServiceFeePayVO payServiceFee(Long userId, String role, Long orderId, String paymentChannel, String remark) {
        ServiceFeePaymentTxService.PaymentCreateResult createResult =
                paymentTxService.createOrReusePayment(userId, role, orderId, paymentChannel, remark);

        if (!wechatPayProperties.isEnabled() && wechatPayProperties.isMockAutoSuccess()) {
            String mockTxId = "MOCK_AUTOSUCCESS_" + createResult.paymentNo();
            paymentTxService.markPaidAndUnlock(createResult.paymentNo(), mockTxId,
                    "service fee auto paid in mock mode");
            log.info("service fee auto paid in mock mode, orderId={}, paymentNo={}",
                    createResult.orderId(), createResult.paymentNo());

            ServiceFeePayVO vo = new ServiceFeePayVO();
            vo.setPaymentNo(createResult.paymentNo());
            vo.setPaymentChannel(createResult.paymentChannel());
            vo.setCodeUrl("mock://auto-success/" + createResult.paymentNo());
            return vo;
        }

        WechatNativeOrderResponse response;
        try {
            response = wechatNativePayService.createNativeOrder(
                    createResult.paymentNo(),
                    createResult.amount(),
                    "AI-Link 服务费");
        } catch (Exception e) {
            log.error("wechat native order create failed, orderId={}, paymentNo={}",
                    createResult.orderId(), createResult.paymentNo(), e);
            try {
                paymentTxService.markCreateFailed(createResult.paymentNo(), e.getMessage());
            } catch (Exception updateEx) {
                log.error("mark payment create failed status failed, paymentNo={}", createResult.paymentNo(), updateEx);
            }
            if (e instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat native order failed");
        }

        try {
            paymentTxService.updatePaymentCodeUrl(createResult.paymentNo(), response.getCodeUrl());
        } catch (Exception e) {
            log.error("update payment codeUrl failed, paymentNo={}", createResult.paymentNo(), e);
        }

        log.info("service fee order created, orderId={}, paymentNo={}, channel={}",
                createResult.orderId(), createResult.paymentNo(), createResult.paymentChannel());

        ServiceFeePayVO vo = new ServiceFeePayVO();
        vo.setPaymentNo(createResult.paymentNo());
        vo.setPaymentChannel(createResult.paymentChannel());
        vo.setCodeUrl(response.getCodeUrl());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleWechatNotify(String signature, String timestamp, String nonce, String body) {
        log.info("received wechat notify callback, timestamp={}, nonce={}", timestamp, nonce);
        WechatNotifyTransaction notifyTx = wechatNativePayService.verifyAndParseNotify(body, timestamp, nonce, signature);
        try {
            if (!"SUCCESS".equalsIgnoreCase(notifyTx.getTradeState())) {
                log.info("wechat notify ignored because trade_state is not SUCCESS, outTradeNo={}, state={}",
                        notifyTx.getOutTradeNo(), notifyTx.getTradeState());
                return successReply();
            }

            PlatformPayment payment = platformPaymentMapper.selectByPaymentNoForUpdate(notifyTx.getOutTradeNo());
            if (payment == null) {
                throw new BizException(ErrorCode.NOT_FOUND.getCode(), "payment order not found");
            }

            if (PlatformPaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
                log.info("wechat notify idempotent hit, paymentNo={} already success", payment.getPaymentNo());
                return successReply();
            }

            int expectedFen = payment.getAmount()
                    .multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();
            if (notifyTx.getTotalFeeFen() == null || notifyTx.getTotalFeeFen() != expectedFen) {
                log.error("wechat notify amount mismatch, paymentNo={}, expectedFen={}, actualFen={}",
                        payment.getPaymentNo(), expectedFen, notifyTx.getTotalFeeFen());
                throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat notify amount mismatch");
            }

            payment.setStatus(PlatformPaymentStatus.SUCCESS.name());
            payment.setTransactionId(notifyTx.getTransactionId());
            payment.setTransactionNo(notifyTx.getTransactionId());
            payment.setPaidTime(LocalDateTime.now());
            platformPaymentMapper.updateById(payment);

            Order order = orderMapper.selectByIdForUpdate(payment.getOrderId());
            if (order == null) {
                throw new BizException(ErrorCode.NOT_FOUND.getCode(), "order not found");
            }
            if (ServiceFeeStatus.PAID.name().equals(order.getServiceFeeStatus())) {
                log.info("order already paid, skip duplicate status update, orderId={}", order.getId());
                return successReply();
            }

            order.setServiceFeeStatus(ServiceFeeStatus.PAID.name());
            order.setServiceFeePaidTime(LocalDateTime.now());
            order.setPayStatus("PAID");
            orderMapper.updateById(order);

            if (!OrderStatus.WAIT_WORKER_ACCEPT.name().equals(order.getStatus())) {
                orderService.updateOrderStatus(order.getEmployerId(), order.getId(), OrderStatus.WAIT_WORKER_ACCEPT,
                        "service fee paid, waiting worker accept");
            }
            log.info("wechat notify processed successfully, orderId={}, paymentNo={}, txId={}",
                    order.getId(), payment.getPaymentNo(), payment.getTransactionId());
            return successReply();
        } catch (Exception e) {
            log.error("wechat notify process failed, outTradeNo={}, txId={}",
                    notifyTx.getOutTradeNo(), notifyTx.getTransactionId(), e);
            throw e;
        }
    }

    private String successReply() {
        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    }
}
