package com.ailink.module.order.controller;

import com.ailink.common.Result;
import com.ailink.common.ErrorCode;
import com.ailink.common.exception.BizException;
import com.ailink.module.order.dto.OrderActionRequest;
import com.ailink.module.order.dto.OrderBatchDeleteRequest;
import com.ailink.module.order.dto.OrderCreateRequest;
import com.ailink.module.order.dto.ServiceFeePayRequest;
import com.ailink.module.order.service.OrderService;
import com.ailink.module.order.service.PlatformConfigService;
import com.ailink.module.order.service.ServiceFeeService;
import com.ailink.module.order.vo.OrderBatchDeleteResultVO;
import com.ailink.module.order.vo.OrderDetailVO;
import com.ailink.module.order.vo.OrderStatusLogVO;
import com.ailink.module.order.vo.OrderVO;
import com.ailink.module.order.vo.FeeConfigVO;
import com.ailink.module.order.vo.ServiceFeePayVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final ServiceFeeService serviceFeeService;
    private final PlatformConfigService platformConfigService;

    public OrderController(OrderService orderService,
                           ServiceFeeService serviceFeeService,
                           PlatformConfigService platformConfigService) {
        this.orderService = orderService;
        this.serviceFeeService = serviceFeeService;
        this.platformConfigService = platformConfigService;
    }

    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody OrderCreateRequest request) {
        Long orderId = orderService.createOrder(SecurityContextUtil.currentUserId(), request);
        return Result.success(Map.of("orderId", orderId));
    }

    @PostMapping("/{orderId}/pay-service-fee")
    public Result<ServiceFeePayVO> payServiceFee(@PathVariable Long orderId,
                                                 @RequestBody(required = false) ServiceFeePayRequest request) {
        ensureClientRole();
        ServiceFeePayVO vo = serviceFeeService.payServiceFee(
                SecurityContextUtil.currentUserId(),
                SecurityContextUtil.currentRole(),
                orderId,
                request == null ? null : request.getPaymentChannel(),
                request == null ? null : request.getRemark());
        return Result.success(vo);
    }

    @PostMapping("/{orderId}/start")
    public Result<Void> startWork(@PathVariable Long orderId, @RequestBody(required = false) OrderActionRequest request) {
        ensureRunnerRole();
        orderService.startWork(SecurityContextUtil.currentUserId(), orderId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @PostMapping("/{orderId}/declare-paid")
    public Result<Void> declarePaid(@PathVariable Long orderId, @RequestBody(required = false) OrderActionRequest request) {
        ensureClientRole();
        orderService.declareExternalPayment(SecurityContextUtil.currentUserId(), orderId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @PostMapping("/{orderId}/accept")
    public Result<Void> acceptWork(@PathVariable Long orderId, @RequestBody(required = false) OrderActionRequest request) {
        ensureRunnerRole();
        orderService.acceptWork(SecurityContextUtil.currentUserId(), orderId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @PostMapping("/{orderId}/reject")
    public Result<Void> rejectWork(@PathVariable Long orderId, @RequestBody(required = false) OrderActionRequest request) {
        ensureRunnerRole();
        orderService.rejectWork(SecurityContextUtil.currentUserId(), orderId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @PostMapping("/{orderId}/complete")
    public Result<Void> confirmComplete(@PathVariable Long orderId, @RequestBody(required = false) OrderActionRequest request) {
        ensureClientRole();
        orderService.confirmComplete(SecurityContextUtil.currentUserId(), orderId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long orderId, @RequestBody(required = false) OrderActionRequest request) {
        ensureClientRole();
        orderService.cancelByEmployer(SecurityContextUtil.currentUserId(), orderId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<OrderVO>> myOrders() {
        return Result.success(orderService.listMyOrders(SecurityContextUtil.currentUserId()));
    }

    @GetMapping("/config/fees")
    public Result<FeeConfigVO> feeConfig() {
        return Result.success(platformConfigService.getFeeConfig());
    }

    @PostMapping("/my/delete")
    public Result<OrderBatchDeleteResultVO> deleteMyOrders(@Valid @RequestBody OrderBatchDeleteRequest request) {
        ensureClientRole();
        return Result.success(orderService.deleteMyFinishedOrders(SecurityContextUtil.currentUserId(), request.getOrderIds()));
    }

    @GetMapping("/{orderId}")
    public Result<OrderVO> detailCompat(@PathVariable Long orderId) {
        return Result.success(orderService.getMyOrder(SecurityContextUtil.currentUserId(), orderId));
    }

    @GetMapping("/{orderId}/detail")
    public Result<OrderDetailVO> detail(@PathVariable Long orderId) {
        return Result.success(orderService.getMyOrderDetail(SecurityContextUtil.currentUserId(), orderId));
    }

    @GetMapping("/{orderId}/logs")
    public Result<List<OrderStatusLogVO>> logs(@PathVariable Long orderId) {
        return Result.success(orderService.listMyOrderLogs(SecurityContextUtil.currentUserId(), orderId));
    }

    private void ensureClientRole() {
        String role = SecurityContextUtil.currentRole();
        if (!"USER".equalsIgnoreCase(role)
                && !"EMPLOYER".equalsIgnoreCase(role)
                && !"CLIENT".equalsIgnoreCase(role)
                && !"WORKER".equalsIgnoreCase(role)) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only client role can perform this action");
        }
    }

    private void ensureRunnerRole() {
        String role = SecurityContextUtil.currentRole();
        if (!"WORKER".equalsIgnoreCase(role) && !"RUNNER".equalsIgnoreCase(role)) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only runner role can perform this action");
        }
    }
}
