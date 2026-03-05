package com.ailink.module.order.service.impl;

import com.ailink.common.exception.BizException;
import com.ailink.common.enums.OrderStatus;
import com.ailink.common.enums.PlatformPaymentStatus;
import com.ailink.common.enums.ServiceFeeStatus;
import com.ailink.module.demand.entity.Demand;
import com.ailink.module.demand.mapper.DemandMapper;
import com.ailink.module.order.dto.OrderCreateRequest;
import com.ailink.module.order.entity.Order;
import com.ailink.module.order.entity.OrderStatusLog;
import com.ailink.module.order.entity.PlatformPayment;
import com.ailink.module.order.mapper.OrderMapper;
import com.ailink.module.order.mapper.OrderStatusLogMapper;
import com.ailink.module.order.mapper.PlatformPaymentMapper;
import com.ailink.module.order.payment.WechatNativeOrderResponse;
import com.ailink.module.order.payment.WechatNativePayService;
import com.ailink.module.order.payment.WechatNotifyTransaction;
import com.ailink.module.order.service.OrderService;
import com.ailink.module.order.service.ServiceFeeService;
import com.ailink.module.order.vo.ServiceFeePayVO;
import com.ailink.config.WechatPayProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ServiceFeeServiceImplIdempotencyTest {

    @Autowired
    private ServiceFeeService serviceFeeService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private DemandMapper demandMapper;

    @Autowired
    private PlatformPaymentMapper platformPaymentMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @MockBean
    private WechatNativePayService wechatNativePayService;

    @Autowired
    private WechatPayProperties wechatPayProperties;

    @SpyBean
    private ServiceFeePaymentTxService paymentTxService;

    @Test
    void shouldKeepSingleUnpaidPaymentUnderRepeatedAndConcurrentPayRequests() throws Exception {
        mockCreateNativeOrder();
        Long orderId = createServiceFeeRequiredOrder();

        ServiceFeePayVO first = serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null);
        ServiceFeePayVO second = serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            List<Callable<ServiceFeePayVO>> tasks = List.of(
                    () -> serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null),
                    () -> serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null),
                    () -> serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null)
            );
            List<Future<ServiceFeePayVO>> futures = executor.invokeAll(tasks);
            for (Future<ServiceFeePayVO> future : futures) {
                ServiceFeePayVO vo = future.get(5, TimeUnit.SECONDS);
                assertEquals(first.getPaymentNo(), vo.getPaymentNo());
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(first.getPaymentNo(), second.getPaymentNo());
        List<PlatformPayment> payments = platformPaymentMapper.selectList(new LambdaQueryWrapper<PlatformPayment>()
                .eq(PlatformPayment::getOrderId, orderId));
        long unpaidCount = payments.stream()
                .filter(p -> PlatformPaymentStatus.UNPAID.name().equals(p.getStatus()))
                .count();
        assertEquals(1L, unpaidCount);
    }

    @Test
    void shouldAllowUserRoleToPayWhenUserIsOrderEmployer() {
        mockCreateNativeOrder();
        Long orderId = createServiceFeeRequiredOrder();

        ServiceFeePayVO payVO = serviceFeeService.payServiceFee(1L, "USER", orderId, "WECHAT_PAY", null);
        assertNotNull(payVO);
        assertNotNull(payVO.getPaymentNo());
    }

    @Test
    void shouldAutoUnlockWhenMockAutoSuccessEnabledAndWechatDisabled() {
        boolean originalMockAutoSuccess = wechatPayProperties.isMockAutoSuccess();
        boolean originalEnabled = wechatPayProperties.isEnabled();
        try {
            wechatPayProperties.setEnabled(false);
            wechatPayProperties.setMockAutoSuccess(true);
            Long orderId = createServiceFeeRequiredOrder();

            ServiceFeePayVO payVO = serviceFeeService.payServiceFee(1L, "USER", orderId, "WECHAT_PAY", null);
            assertNotNull(payVO);
            assertTrue(String.valueOf(payVO.getCodeUrl()).startsWith("mock://auto-success/"));

            Order order = orderMapper.selectById(orderId);
            assertEquals(ServiceFeeStatus.PAID.name(), order.getServiceFeeStatus());
            assertEquals(OrderStatus.WAIT_WORKER_ACCEPT.name(), order.getStatus());

            PlatformPayment payment = platformPaymentMapper.selectOne(new LambdaQueryWrapper<PlatformPayment>()
                    .eq(PlatformPayment::getPaymentNo, payVO.getPaymentNo())
                    .last("limit 1"));
            assertNotNull(payment);
            assertEquals(PlatformPaymentStatus.SUCCESS.name(), payment.getStatus());
        } finally {
            wechatPayProperties.setEnabled(originalEnabled);
            wechatPayProperties.setMockAutoSuccess(originalMockAutoSuccess);
        }
    }

    @Test
    void shouldBeIdempotentForRepeatedAndConcurrentWechatNotify() throws Exception {
        mockCreateNativeOrder();
        Long orderId = createServiceFeeRequiredOrder();
        ServiceFeePayVO payVO = serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null);

        PlatformPayment payment = platformPaymentMapper.selectOne(new LambdaQueryWrapper<PlatformPayment>()
                .eq(PlatformPayment::getPaymentNo, payVO.getPaymentNo())
                .last("limit 1"));
        assertNotNull(payment);

        when(wechatNativePayService.verifyAndParseNotify(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> buildSuccessNotify(payment.getPaymentNo(), "TX-IDEMP-001", 10000));

        for (int i = 0; i < 5; i++) {
            String reply = serviceFeeService.handleWechatNotify("sign", "1710000000", "nonce", "serial", "{}");
            assertTrue(reply.contains("SUCCESS"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            List<Callable<String>> tasks = List.of(
                    () -> serviceFeeService.handleWechatNotify("sign", "1710000000", "nonce", "serial", "{}"),
                    () -> serviceFeeService.handleWechatNotify("sign", "1710000000", "nonce", "serial", "{}"),
                    () -> serviceFeeService.handleWechatNotify("sign", "1710000000", "nonce", "serial", "{}")
            );
            List<Future<String>> futures = executor.invokeAll(tasks);
            List<Throwable> errors = new ArrayList<>();
            for (Future<String> future : futures) {
                try {
                    String reply = future.get(5, TimeUnit.SECONDS);
                    assertTrue(reply.contains("SUCCESS"));
                } catch (ExecutionException ex) {
                    errors.add(ex.getCause() == null ? ex : ex.getCause());
                }
            }
            assertTrue(errors.isEmpty(), "concurrent notify should not fail: " + errors);
        } finally {
            executor.shutdownNow();
        }

        Order order = orderMapper.selectById(orderId);
        assertEquals(ServiceFeeStatus.PAID.name(), order.getServiceFeeStatus());
        assertEquals(OrderStatus.WAIT_WORKER_ACCEPT.name(), order.getStatus());

        PlatformPayment after = platformPaymentMapper.selectById(payment.getId());
        assertEquals(PlatformPaymentStatus.SUCCESS.name(), after.getStatus());

        Long paidLogCount = orderStatusLogMapper.selectCount(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, orderId)
                .eq(OrderStatusLog::getToStatus, OrderStatus.WAIT_WORKER_ACCEPT.name()));
        assertEquals(1L, paidLogCount);
    }

    @Test
    void shouldKeepPaymentRecordAndMarkCreateFailedWhenWechatCreateThrows() {
        Long orderId = createServiceFeeRequiredOrder();
        when(wechatNativePayService.createNativeOrder(anyString(), any(BigDecimal.class), anyString()))
                .thenThrow(new RuntimeException("wechat timeout"));

        assertThrows(BizException.class,
                () -> serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null));

        PlatformPayment payment = platformPaymentMapper.selectOne(new LambdaQueryWrapper<PlatformPayment>()
                .eq(PlatformPayment::getOrderId, orderId)
                .last("limit 1"));
        assertNotNull(payment);
        assertEquals(PlatformPaymentStatus.CREATE_FAILED.name(), payment.getStatus());
    }

    @Test
    void shouldCreateNewPaymentNoWhenPreviousPaymentIsCreateFailed() {
        Long orderId = createServiceFeeRequiredOrder();
        when(wechatNativePayService.createNativeOrder(anyString(), any(BigDecimal.class), anyString()))
                .thenThrow(new RuntimeException("wechat timeout"))
                .thenAnswer(invocation -> {
                    WechatNativeOrderResponse response = new WechatNativeOrderResponse();
                    response.setCodeUrl("mock://wechat-pay/" + invocation.getArgument(0, String.class));
                    return response;
                });

        assertThrows(BizException.class,
                () -> serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null));

        PlatformPayment failed = platformPaymentMapper.selectOne(new LambdaQueryWrapper<PlatformPayment>()
                .eq(PlatformPayment::getOrderId, orderId)
                .eq(PlatformPayment::getStatus, PlatformPaymentStatus.CREATE_FAILED.name())
                .orderByDesc(PlatformPayment::getId)
                .last("limit 1"));
        assertNotNull(failed);

        ServiceFeePayVO retry = serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null);
        assertNotNull(retry);
        assertNotEquals(failed.getPaymentNo(), retry.getPaymentNo());

        List<PlatformPayment> all = platformPaymentMapper.selectList(new LambdaQueryWrapper<PlatformPayment>()
                .eq(PlatformPayment::getOrderId, orderId)
                .orderByAsc(PlatformPayment::getId));
        assertEquals(2, all.size());
        assertEquals(PlatformPaymentStatus.CREATE_FAILED.name(), all.get(0).getStatus());
        assertEquals(PlatformPaymentStatus.UNPAID.name(), all.get(1).getStatus());
    }

    @Test
    void shouldStillHandleNotifyWhenCodeUrlUpdateFailsAfterWechatOrderCreated() {
        mockCreateNativeOrder();
        Long orderId = createServiceFeeRequiredOrder();
        doThrow(new RuntimeException("db write failed"))
                .when(paymentTxService).updatePaymentCodeUrl(anyString(), anyString());

        ServiceFeePayVO payVO = serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null);
        assertNotNull(payVO);
        assertNotNull(payVO.getPaymentNo());

        PlatformPayment payment = platformPaymentMapper.selectOne(new LambdaQueryWrapper<PlatformPayment>()
                .eq(PlatformPayment::getPaymentNo, payVO.getPaymentNo())
                .last("limit 1"));
        assertNotNull(payment);
        assertEquals(PlatformPaymentStatus.UNPAID.name(), payment.getStatus());

        when(wechatNativePayService.verifyAndParseNotify(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> buildSuccessNotify(payVO.getPaymentNo(), "TX-UPDFAIL-001", 10000));

        String reply = serviceFeeService.handleWechatNotify("sign", "1710000000", "nonce", "serial", "{}");
        assertTrue(reply.contains("SUCCESS"));

        Order order = orderMapper.selectById(orderId);
        assertEquals(ServiceFeeStatus.PAID.name(), order.getServiceFeeStatus());
        assertEquals(OrderStatus.WAIT_WORKER_ACCEPT.name(), order.getStatus());
    }

    @Test
    void shouldRejectPayWhenExistingSuccessPaymentFound() {
        mockCreateNativeOrder();
        Long orderId = createServiceFeeRequiredOrder();
        ServiceFeePayVO payVO = serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null);

        when(wechatNativePayService.verifyAndParseNotify(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> buildSuccessNotify(payVO.getPaymentNo(), "TX-SUCCESS-001", 10000));
        serviceFeeService.handleWechatNotify("sign", "1710000000", "nonce", "serial", "{}");

        BizException exception = assertThrows(BizException.class,
                () -> serviceFeeService.payServiceFee(1L, "EMPLOYER", orderId, "WECHAT_PAY", null));
        assertTrue(exception.getMessage().contains("already paid"));
    }

    private void mockCreateNativeOrder() {
        when(wechatNativePayService.createNativeOrder(anyString(), any(BigDecimal.class), anyString()))
                .thenAnswer(invocation -> {
                    WechatNativeOrderResponse response = new WechatNativeOrderResponse();
                    response.setCodeUrl("mock://wechat-pay/" + invocation.getArgument(0, String.class));
                    return response;
                });
    }

    private Long createServiceFeeRequiredOrder() {
        Long demandId = createDemandForTest();
        OrderCreateRequest request = new OrderCreateRequest();
        request.setDemandId(demandId);
        request.setWorkerProfileId(20L);
        request.setAmount(new BigDecimal("1000.00"));
        request.setPaymentChannel("WECHAT_PAY");
        return orderService.createOrder(1L, request);
    }

    private Long createDemandForTest() {
        Demand demand = new Demand();
        demand.setUserId(1L);
        demand.setTargetCountry("SG");
        demand.setCategory("translation");
        demand.setBudget(new BigDecimal("1000.00"));
        demand.setDescription("test demand " + System.nanoTime());
        demand.setDeadline(LocalDateTime.now().plusDays(7));
        demand.setStatus("OPEN");
        demandMapper.insert(demand);
        return demand.getId();
    }

    private WechatNotifyTransaction buildSuccessNotify(String paymentNo, String txId, Integer totalFen) {
        WechatNotifyTransaction tx = new WechatNotifyTransaction();
        tx.setOutTradeNo(paymentNo);
        tx.setTransactionId(txId);
        tx.setTradeState("SUCCESS");
        tx.setTotalFeeFen(totalFen);
        tx.setSuccessTime("2026-02-28T00:00:00+08:00");
        return tx;
    }
}
