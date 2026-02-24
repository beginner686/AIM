package com.ailink.module.order.controller;

import com.ailink.common.Result;
import com.ailink.module.order.dto.OrderActionRequest;
import com.ailink.module.order.dto.OrderCreateRequest;
import com.ailink.module.order.dto.OrderDisputeRequest;
import com.ailink.module.order.service.OrderService;
import com.ailink.module.order.vo.OrderVO;
import com.ailink.module.order.vo.OrderStatusLogVO;
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

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody OrderCreateRequest request) {
        Long orderId = orderService.createOrder(SecurityContextUtil.currentUserId(), request);
        return Result.success(Map.of("orderId", orderId));
    }

    @PostMapping("/{orderId}/escrow")
    public Result<Void> payEscrow(@PathVariable Long orderId, @RequestBody(required = false) OrderActionRequest request) {
        orderService.payEscrow(SecurityContextUtil.currentUserId(), orderId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @PostMapping("/{orderId}/start")
    public Result<Void> startWork(@PathVariable Long orderId, @RequestBody(required = false) OrderActionRequest request) {
        orderService.startWork(SecurityContextUtil.currentUserId(), orderId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @PostMapping("/{orderId}/complete")
    public Result<Void> confirmComplete(@PathVariable Long orderId, @RequestBody(required = false) OrderActionRequest request) {
        orderService.confirmComplete(SecurityContextUtil.currentUserId(), orderId, request == null ? null : request.getRemark());
        return Result.success();
    }

    @PostMapping("/{orderId}/dispute")
    public Result<Void> dispute(@PathVariable Long orderId, @Valid @RequestBody OrderDisputeRequest request) {
        orderService.raiseDispute(SecurityContextUtil.currentUserId(), orderId, request.getReason());
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<OrderVO>> myOrders() {
        return Result.success(orderService.listMyOrders(SecurityContextUtil.currentUserId()));
    }

    @GetMapping("/{orderId}")
    public Result<OrderVO> detail(@PathVariable Long orderId) {
        return Result.success(orderService.getMyOrder(SecurityContextUtil.currentUserId(), orderId));
    }

    @GetMapping("/{orderId}/logs")
    public Result<List<OrderStatusLogVO>> logs(@PathVariable Long orderId) {
        return Result.success(orderService.listMyOrderLogs(SecurityContextUtil.currentUserId(), orderId));
    }
}
