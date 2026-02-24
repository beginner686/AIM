package com.ailink.module.escrow.controller;

import com.ailink.common.Result;
import com.ailink.module.escrow.service.EscrowService;
import com.ailink.module.escrow.vo.EscrowRecordVO;
import com.ailink.module.order.service.OrderService;
import com.ailink.security.SecurityContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/escrow")
public class EscrowController {

    private final EscrowService escrowService;
    private final OrderService orderService;

    public EscrowController(EscrowService escrowService, OrderService orderService) {
        this.escrowService = escrowService;
        this.orderService = orderService;
    }

    @GetMapping("/order/{orderId}")
    public Result<EscrowRecordVO> getByOrderId(@PathVariable Long orderId) {
        orderService.getMyOrder(SecurityContextUtil.currentUserId(), orderId);
        return Result.success(escrowService.getByOrderId(orderId));
    }
}
