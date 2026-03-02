package com.ailink.module.order.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.enums.OrderStatus;
import com.ailink.common.enums.PlatformPaymentStatus;
import com.ailink.common.enums.ServiceFeeStatus;
import com.ailink.common.exception.BizException;
import com.ailink.module.order.entity.Order;
import com.ailink.module.order.entity.PlatformPayment;
import com.ailink.module.order.mapper.OrderMapper;
import com.ailink.module.order.mapper.PlatformPaymentMapper;
import com.ailink.module.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ServiceFeePaymentTxService {

    private static final Logger log = LoggerFactory.getLogger(ServiceFeePaymentTxService.class);

    private final OrderMapper orderMapper;
    private final PlatformPaymentMapper platformPaymentMapper;
    private final OrderService orderService;

    public ServiceFeePaymentTxService(OrderMapper orderMapper,
                                      PlatformPaymentMapper platformPaymentMapper,
                                      OrderService orderService) {
        this.orderMapper = orderMapper;
        this.platformPaymentMapper = platformPaymentMapper;
        this.orderService = orderService;
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentCreateResult createOrReusePayment(Long userId, String role, Long orderId, String paymentChannel, String remark) {
        Order order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "order not found");
        }
        if (!"USER".equalsIgnoreCase(role)
                && !"EMPLOYER".equalsIgnoreCase(role)
                && !"CLIENT".equalsIgnoreCase(role)) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only client can pay service fee");
        }
        if (!userId.equals(order.getEmployerId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only order client can pay service fee");
        }

        PlatformPayment successPayment = platformPaymentMapper.selectSuccessByOrderIdForUpdate(orderId);
        if (successPayment != null) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "service fee already paid");
        }

        if (!OrderStatus.SERVICE_FEE_REQUIRED.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "service fee can only be paid when status is SERVICE_FEE_REQUIRED");
        }
        if (ServiceFeeStatus.PAID.name().equals(order.getServiceFeeStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "service fee already paid");
        }

        String channel = StringUtils.hasText(paymentChannel) ? paymentChannel.trim().toUpperCase() : "WECHAT_PAY";

        PlatformPayment unpaidPayment = platformPaymentMapper.selectUnpaidByOrderIdForUpdate(orderId);
        if (unpaidPayment != null) {
            log.info("service fee payment reused, orderId={}, paymentNo={}", orderId, unpaidPayment.getPaymentNo());
            return new PaymentCreateResult(
                    unpaidPayment.getOrderId(),
                    unpaidPayment.getPaymentNo(),
                    unpaidPayment.getPaymentChannel(),
                    unpaidPayment.getAmount());
        }

        PlatformPayment newPayment = new PlatformPayment();
        newPayment.setPaymentNo(buildPaymentNo(orderId));
        newPayment.setOrderId(orderId);
        newPayment.setPayerId(userId);
        newPayment.setPaymentChannel(channel);
        newPayment.setAmount(order.getServiceFeeAmount());
        newPayment.setStatus(PlatformPaymentStatus.UNPAID.name());
        newPayment.setRemark(StringUtils.hasText(remark) ? remark : "service fee pending payment");
        try {
            platformPaymentMapper.insert(newPayment);
            return new PaymentCreateResult(newPayment.getOrderId(), newPayment.getPaymentNo(), newPayment.getPaymentChannel(), newPayment.getAmount());
        } catch (DuplicateKeyException e) {
            PlatformPayment existing = platformPaymentMapper.selectUnpaidByOrderIdForUpdate(orderId);
            if (existing == null) {
                throw e;
            }
            return new PaymentCreateResult(existing.getOrderId(), existing.getPaymentNo(), existing.getPaymentChannel(), existing.getAmount());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markCreateFailed(String paymentNo, String reason) {
        PlatformPayment payment = platformPaymentMapper.selectByPaymentNoForUpdate(paymentNo);
        if (payment == null) {
            return;
        }
        if (PlatformPaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            return;
        }
        payment.setStatus(PlatformPaymentStatus.CREATE_FAILED.name());
        platformPaymentMapper.updateById(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updatePaymentCodeUrl(String paymentNo, String codeUrl) {
        PlatformPayment payment = platformPaymentMapper.selectByPaymentNoForUpdate(paymentNo);
        if (payment == null) {
            return;
        }
        payment.setCodeUrl(codeUrl);
        platformPaymentMapper.updateById(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markPaidAndUnlock(String paymentNo, String transactionId, String remark) {
        PlatformPayment payment = platformPaymentMapper.selectByPaymentNoForUpdate(paymentNo);
        if (payment == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "payment order not found");
        }
        if (PlatformPaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            return;
        }

        String txId = StringUtils.hasText(transactionId)
                ? transactionId
                : ("MOCK_" + payment.getPaymentNo());
        payment.setStatus(PlatformPaymentStatus.SUCCESS.name());
        payment.setTransactionId(txId);
        payment.setTransactionNo(txId);
        payment.setPaidTime(LocalDateTime.now());
        if (StringUtils.hasText(remark)) {
            payment.setRemark(remark);
        }
        platformPaymentMapper.updateById(payment);

        Order order = orderMapper.selectByIdForUpdate(payment.getOrderId());
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "order not found");
        }
        if (ServiceFeeStatus.PAID.name().equals(order.getServiceFeeStatus())) {
            return;
        }

        order.setServiceFeeStatus(ServiceFeeStatus.PAID.name());
        order.setServiceFeePaidTime(LocalDateTime.now());
        order.setPayStatus("PAID");
        orderMapper.updateById(order);

        if (!OrderStatus.SERVICE_FEE_PAID.name().equals(order.getStatus())) {
            String statusRemark = StringUtils.hasText(remark) ? remark : "service fee paid";
            orderService.updateOrderStatus(order.getEmployerId(), order.getId(), OrderStatus.SERVICE_FEE_PAID, statusRemark);
        }
    }

    private String buildPaymentNo(Long orderId) {
        return "PAY" + orderId + System.currentTimeMillis();
    }

    public record PaymentCreateResult(Long orderId, String paymentNo, String paymentChannel, java.math.BigDecimal amount) {
    }
}
