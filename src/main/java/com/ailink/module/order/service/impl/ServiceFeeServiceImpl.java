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
import org.springframework.util.StringUtils;

@Service
public class ServiceFeeServiceImpl implements ServiceFeeService {

    private static final Logger log = LoggerFactory.getLogger(ServiceFeeServiceImpl.class);

    private final OrderMapper orderMapper;
    private final PlatformPaymentMapper platformPaymentMapper;
    private final OrderService orderService;
    private final WechatNativePayService wechatNativePayService;
    private final ServiceFeePaymentTxService paymentTxService;
    private final WechatPayProperties wechatPayProperties;
    private final com.ailink.module.order.mapper.PaymentNotifyLogMapper paymentNotifyLogMapper;

    public ServiceFeeServiceImpl(OrderMapper orderMapper,
            PlatformPaymentMapper platformPaymentMapper,
            OrderService orderService,
            WechatNativePayService wechatNativePayService,
            ServiceFeePaymentTxService paymentTxService,
            WechatPayProperties wechatPayProperties,
            com.ailink.module.order.mapper.PaymentNotifyLogMapper paymentNotifyLogMapper) {
        this.orderMapper = orderMapper;
        this.platformPaymentMapper = platformPaymentMapper;
        this.orderService = orderService;
        this.wechatNativePayService = wechatNativePayService;
        this.paymentTxService = paymentTxService;
        this.wechatPayProperties = wechatPayProperties;
        this.paymentNotifyLogMapper = paymentNotifyLogMapper;
    }

    @Override
    public ServiceFeePayVO payServiceFee(Long userId, String role, Long orderId, String paymentChannel, String remark) {
        ServiceFeePaymentTxService.PaymentCreateResult createResult = paymentTxService.createOrReusePayment(userId,
                role, orderId, paymentChannel, remark);

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
    public String handleWechatNotify(String signature, String timestamp, String nonce, String serial, String body) {
        log.info("received wechat notify callback, timestamp={}, nonce={}, serial={}", timestamp, nonce, serial);
        WechatNotifyTransaction notifyTx = wechatNativePayService.verifyAndParseNotify(body, timestamp, nonce, serial,
                signature);
        return handleWechatNotifyInTx(notifyTx);
    }

    @Transactional(rollbackFor = Exception.class)
    protected String handleWechatNotifyInTx(WechatNotifyTransaction notifyTx) {
        if (!StringUtils.hasText(notifyTx.getEventId())) {
            log.warn("wechat notify missing event_id, fallback to old logic");
        } else {
            com.ailink.module.order.entity.PaymentNotifyLog existLog = paymentNotifyLogMapper
                    .selectByEventId(notifyTx.getEventId());
            if (existLog != null) {
                log.info("wechat notify event_id={} already processed, status={}", notifyTx.getEventId(),
                        existLog.getProcessStatus());
                return successReply();
            }

            com.ailink.module.order.entity.PaymentNotifyLog newLog = new com.ailink.module.order.entity.PaymentNotifyLog();
            newLog.setEventId(notifyTx.getEventId());
            newLog.setPaymentNo(notifyTx.getOutTradeNo());
            newLog.setTransactionId(notifyTx.getTransactionId());
            newLog.setNotifyContent(notifyTx.getRawContent());
            newLog.setProcessStatus("PENDING");
            try {
                paymentNotifyLogMapper.insert(newLog);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                log.info("wechat notify event_id={} concurrent duplicate hit", notifyTx.getEventId());
                return successReply();
            }
        }

        try {
            if (!"SUCCESS".equalsIgnoreCase(notifyTx.getTradeState())) {
                log.info("wechat notify ignored because trade_state is not SUCCESS, outTradeNo={}, state={}",
                        notifyTx.getOutTradeNo(), notifyTx.getTradeState());
                updateNotifyLogStatus(notifyTx.getEventId(), "IGNORED", "trade_state is not SUCCESS");
                return successReply();
            }

            PlatformPayment paymentSnapshot = platformPaymentMapper.selectByPaymentNo(notifyTx.getOutTradeNo());
            if (paymentSnapshot == null) {
                updateNotifyLogStatus(notifyTx.getEventId(), "FAIL", "payment order not found");
                throw new BizException(ErrorCode.NOT_FOUND.getCode(), "payment order not found");
            }
            Order order = orderMapper.selectByIdForUpdate(paymentSnapshot.getOrderId());
            if (order == null) {
                updateNotifyLogStatus(notifyTx.getEventId(), "FAIL", "order not found");
                throw new BizException(ErrorCode.NOT_FOUND.getCode(), "order not found");
            }

            PlatformPayment payment = platformPaymentMapper.selectByPaymentNoForUpdate(notifyTx.getOutTradeNo());
            if (payment == null) {
                updateNotifyLogStatus(notifyTx.getEventId(), "FAIL", "payment order not found");
                throw new BizException(ErrorCode.NOT_FOUND.getCode(), "payment order not found");
            }
            if (!order.getId().equals(payment.getOrderId())) {
                updateNotifyLogStatus(notifyTx.getEventId(), "FAIL", "payment order relation mismatch");
                throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "payment order relation mismatch");
            }

            if (PlatformPaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
                log.info("wechat notify idempotent hit, paymentNo={} already success", payment.getPaymentNo());
                updateNotifyLogStatus(notifyTx.getEventId(), "SUCCESS", "already success");
                return successReply();
            }

            int expectedFen = payment.getAmount()
                    .multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();
            if (notifyTx.getTotalFeeFen() == null || notifyTx.getTotalFeeFen() != expectedFen) {
                log.error("wechat notify amount mismatch, paymentNo={}, expectedFen={}, actualFen={}",
                        payment.getPaymentNo(), expectedFen, notifyTx.getTotalFeeFen());
                updateNotifyLogStatus(notifyTx.getEventId(), "FAIL", "amount mismatch");
                throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "wechat notify amount mismatch");
            }

            payment.setStatus(PlatformPaymentStatus.SUCCESS.name());
            payment.setTransactionId(notifyTx.getTransactionId());
            payment.setTransactionNo(notifyTx.getTransactionId());
            payment.setPaidTime(LocalDateTime.now());
            platformPaymentMapper.updateById(payment);

            if (ServiceFeeStatus.PAID.name().equals(order.getServiceFeeStatus())) {
                log.info("order already paid, skip duplicate status update, orderId={}", order.getId());
                updateNotifyLogStatus(notifyTx.getEventId(), "SUCCESS", "order already paid");
                return successReply();
            }

            order.setServiceFeeStatus(ServiceFeeStatus.PAID.name());
            order.setServiceFeePaidTime(LocalDateTime.now());
            order.setPayStatus("PAID");
            if (OrderStatus.CLOSED.name().equals(order.getStatus())) {
                order.setRiskStatus("ABNORMAL");
                String closedReason = order.getClosedReason();
                if (closedReason == null || !closedReason.contains("PAID_AFTER_CLOSED")) {
                    order.setClosedReason((closedReason == null ? "" : closedReason + " | ") + "PAID_AFTER_CLOSED");
                }
                orderMapper.updateById(order);
                log.warn("payment received after order closed, paymentNo={}, orderId={}",
                        payment.getPaymentNo(), order.getId());
                updateNotifyLogStatus(notifyTx.getEventId(), "SUCCESS", "paid after closed");
                return successReply();
            }
            orderMapper.updateById(order);

            if (OrderStatus.SERVICE_FEE_REQUIRED.name().equals(order.getStatus())) {
                orderService.updateOrderStatus(order.getEmployerId(), order.getId(), OrderStatus.SERVICE_FEE_PAID,
                        "service fee paid");
                order = orderMapper.selectByIdForUpdate(order.getId());
            }
            if (!OrderStatus.WAIT_WORKER_ACCEPT.name().equals(order.getStatus())) {
                orderService.updateOrderStatus(order.getEmployerId(), order.getId(), OrderStatus.WAIT_WORKER_ACCEPT,
                        "service fee paid, waiting worker accept");
            }
            log.info("wechat notify processed successfully, orderId={}, paymentNo={}, txId={}",
                    order.getId(), payment.getPaymentNo(), payment.getTransactionId());
            updateNotifyLogStatus(notifyTx.getEventId(), "SUCCESS", null);
            return successReply();
        } catch (Exception e) {
            log.error("wechat notify process failed, outTradeNo={}, txId={}",
                    notifyTx.getOutTradeNo(), notifyTx.getTransactionId(), e);
            updateNotifyLogStatus(notifyTx.getEventId(), "FAIL", e.getMessage());
            throw e;
        }
    }

    private void updateNotifyLogStatus(String eventId, String status, String errorMsg) {
        if (!StringUtils.hasText(eventId))
            return;
        com.ailink.module.order.entity.PaymentNotifyLog logEntity = paymentNotifyLogMapper.selectByEventId(eventId);
        if (logEntity != null) {
            logEntity.setProcessStatus(status);
            if (errorMsg != null) {
                logEntity.setErrorMsg(errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg);
            }
            paymentNotifyLogMapper.updateById(logEntity);
        }
    }

    private String successReply() {
        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    }
}
