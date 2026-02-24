package com.ailink.module.order.controller;

import com.ailink.common.Result;
import com.ailink.module.order.dto.AdminForceCloseRequest;
import com.ailink.module.order.service.OrderService;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{orderId}/force-close")
    public Result<Void> forceClose(@PathVariable Long orderId, @Valid @RequestBody AdminForceCloseRequest request) {
        orderService.closeOrderByAdmin(SecurityContextUtil.currentUserId(), orderId, request.getReason());
        return Result.success();
    }
}
