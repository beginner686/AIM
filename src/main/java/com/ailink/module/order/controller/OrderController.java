package com.ailink.module.order.controller;

import com.ailink.common.Result;
import com.ailink.common.ErrorCode;
import com.ailink.common.exception.BizException;
import com.ailink.module.order.dto.OrderActionRequest;
import com.ailink.module.order.dto.OrderCreateRequest;
import com.ailink.module.order.dto.ServiceFeePayRequest;
import com.ailink.module.order.service.OrderService;
import com.ailink.module.order.service.ServiceFeeService;
import com.ailink.module.order.vo.OrderDetailVO;
import com.ailink.module.order.vo.OrderStatusLogVO;
import com.ailink.module.order.vo.OrderVO;
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

    public OrderController(OrderService orderService, ServiceFeeService serviceFeeService) {
        this.orderService = orderService;
        this.serviceFeeService = serviceFeeService;
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

    @PostMapping("/{orderId}/complete")
    public Result<Void> confirmComplete(@PathVariable Long orderId, @RequestBody(required = false) OrderActionRequest request) {
        ensureClientRole();
        orderService.confirmComplete(SecurityContextUtil.currentUserId(), orderId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<OrderVO>> myOrders() {
        return Result.success(orderService.listMyOrders(SecurityContextUtil.currentUserId()));
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
                && !"CLIENT".equalsIgnoreCase(role)) {
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
