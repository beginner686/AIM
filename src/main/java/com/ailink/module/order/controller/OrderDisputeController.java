package com.ailink.module.order.controller;

import com.ailink.common.Result;
import com.ailink.module.order.dto.OrderDisputeRequest;
import com.ailink.module.order.service.DisputeTicketService;
import com.ailink.module.order.vo.DisputeTicketVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order/{orderId}/dispute")
public class OrderDisputeController {

    private final DisputeTicketService disputeTicketService;

    public OrderDisputeController(DisputeTicketService disputeTicketService) {
        this.disputeTicketService = disputeTicketService;
    }

    @PostMapping
    public Result<DisputeTicketVO> create(@PathVariable Long orderId, @Valid @RequestBody OrderDisputeRequest request) {
        return Result.success(disputeTicketService.create(
                SecurityContextUtil.currentUserId(),
                orderId,
                request.getReason(),
                request.getEvidenceUrl()));
    }

    @GetMapping
    public Result<List<DisputeTicketVO>> list(@PathVariable Long orderId) {
        return Result.success(disputeTicketService.listByOrder(SecurityContextUtil.currentUserId(), orderId));
    }
}
