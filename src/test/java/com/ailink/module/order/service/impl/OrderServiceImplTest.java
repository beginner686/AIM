package com.ailink.module.order.service.impl;

import com.ailink.common.enums.OrderStatus;
import com.ailink.common.exception.BizException;
import com.ailink.module.order.dto.OrderCreateRequest;
import com.ailink.module.order.entity.Order;
import com.ailink.module.order.mapper.OrderMapper;
import com.ailink.module.order.service.OrderService;
import com.ailink.module.order.vo.OrderDetailVO;
import com.ailink.module.worker.entity.RunnerPaymentProfile;
import com.ailink.module.worker.mapper.RunnerPaymentProfileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceImplTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RunnerPaymentProfileMapper runnerPaymentProfileMapper;

    @Test
    void shouldTransitLegalPath() {
        Long orderId = insertOrderWithStatus(OrderStatus.CREATED.name(), "REQUIRED");
        orderService.updateOrderStatus(1L, orderId, OrderStatus.SERVICE_FEE_REQUIRED, "test");
        orderService.updateOrderStatus(1L, orderId, OrderStatus.SERVICE_FEE_PAID, "test");
        orderService.updateOrderStatus(1L, orderId, OrderStatus.MATCH_UNLOCKED, "test");
        orderService.updateOrderStatus(2L, orderId, OrderStatus.IN_PROGRESS, "test");
        orderService.updateOrderStatus(1L, orderId, OrderStatus.COMPLETED, "test");
        orderService.updateOrderStatus(1L, orderId, OrderStatus.CLOSED, "test");

        Order order = orderMapper.selectById(orderId);
        assertEquals(OrderStatus.CLOSED.name(), order.getStatus());
    }

    @Test
    void shouldRejectIllegalTransitionCreatedToCompleted() {
        Long orderId = insertOrderWithStatus(OrderStatus.CREATED.name(), "REQUIRED");
        assertThrows(BizException.class,
                () -> orderService.updateOrderStatus(1L, orderId, OrderStatus.COMPLETED, "illegal"));
    }

    @Test
    void shouldRejectIllegalTransitionRequiredToInProgress() {
        Long orderId = insertOrderWithStatus(OrderStatus.SERVICE_FEE_REQUIRED.name(), "REQUIRED");
        assertThrows(BizException.class,
                () -> orderService.updateOrderStatus(1L, orderId, OrderStatus.IN_PROGRESS, "illegal"));
    }

    @Test
    void shouldRejectIllegalTransitionCompletedToPaid() {
        Long orderId = insertOrderWithStatus(OrderStatus.COMPLETED.name(), "PAID");
        assertThrows(BizException.class,
                () -> orderService.updateOrderStatus(1L, orderId, OrderStatus.SERVICE_FEE_PAID, "illegal"));
    }

    @Test
    void shouldCreateOrderAndMoveToServiceFeeRequired() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setDemandId(10L);
        request.setWorkerProfileId(20L);
        request.setAmount(new BigDecimal("1000.00"));
        request.setPaymentChannel("WECHAT_PAY");

        Long orderId = orderService.createOrder(1L, request);
        Order order = orderMapper.selectById(orderId);
        assertNotNull(order);
        assertEquals(OrderStatus.SERVICE_FEE_REQUIRED.name(), order.getStatus());
        assertEquals("REQUIRED", order.getServiceFeeStatus());
        assertEquals(new BigDecimal("100.00"), order.getServiceFeeAmount().setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void shouldAutoUnlockBeforeStartWork() {
        Long orderId = insertOrderWithStatus(OrderStatus.SERVICE_FEE_PAID.name(), "PAID");
        orderService.startWork(2L, orderId, "start");
        Order order = orderMapper.selectById(orderId);
        assertEquals(OrderStatus.IN_PROGRESS.name(), order.getStatus());
    }

    @Test
    void shouldHideSensitiveFieldsWhenServiceFeeUnpaid() {
        Long orderId = insertOrderWithStatus(OrderStatus.SERVICE_FEE_REQUIRED.name(), "REQUIRED");
        OrderDetailVO detailVO = orderService.getMyOrderDetail(1L, orderId);
        assertFalse(detailVO.getShowContact());
        assertNull(detailVO.getShowChat());
        assertNull(detailVO.getRunnerContact());
        assertNull(detailVO.getRunnerPaymentProfile());
    }

    @Test
    void shouldShowSensitiveFieldsWhenServiceFeePaid() {
        Long orderId = insertOrderWithStatus(OrderStatus.SERVICE_FEE_PAID.name(), "PAID");
        RunnerPaymentProfile profile = new RunnerPaymentProfile();
        profile.setUserId(2L);
        profile.setPaypalEmail("runner@paypal.test");
        profile.setWiseLink("https://wise.test/runner");
        profile.setPaymentUrl("https://pay.test/runner");
        profile.setCurrency("USD");
        profile.setVerified(1);
        runnerPaymentProfileMapper.insert(profile);

        OrderDetailVO detailVO = orderService.getMyOrderDetail(1L, orderId);
        assertTrue(detailVO.getShowContact());
        assertNotNull(detailVO.getShowChat());
        assertEquals("runner@ailink.test", detailVO.getRunnerContact());
        assertNotNull(detailVO.getRunnerPaymentProfile());
        assertEquals("runner@paypal.test", detailVO.getRunnerPaymentProfile().getPaypalEmail());
    }

    private Long insertOrderWithStatus(String status, String serviceFeeStatus) {
        Order order = new Order();
        order.setOrderNo("TEST" + System.nanoTime());
        order.setDemandId(10L);
        order.setEmployerId(1L);
        order.setWorkerProfileId(20L);
        order.setWorkerUserId(2L);
        order.setAmount(new BigDecimal("1000.00"));
        order.setStatus(status);
        order.setServiceFeeStatus(serviceFeeStatus);
        order.setServiceFeeAmount(new BigDecimal("100.00"));
        order.setPayStatus("UNPAID");
        order.setPaymentChannel("WECHAT_PAY");
        order.setPlatformFeeRate(new BigDecimal("0.06"));
        order.setPlatformFee(new BigDecimal("60.00"));
        order.setEscrowFeeRate(BigDecimal.ZERO);
        order.setEscrowFee(BigDecimal.ZERO);
        order.setWorkerIncome(new BigDecimal("940.00"));
        order.setRiskStatus("NORMAL");
        orderMapper.insert(order);
        return order.getId();
    }
}
