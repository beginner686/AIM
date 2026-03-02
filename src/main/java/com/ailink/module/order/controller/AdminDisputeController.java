package com.ailink.module.order.controller;

import com.ailink.common.Result;
import com.ailink.module.order.dto.DisputeResolveRequest;
import com.ailink.module.order.service.DisputeTicketService;
import com.ailink.module.order.vo.DisputeTicketVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dispute")
public class AdminDisputeController {

    private final DisputeTicketService disputeTicketService;

    public AdminDisputeController(DisputeTicketService disputeTicketService) {
        this.disputeTicketService = disputeTicketService;
    }

    @PostMapping("/{ticketId}/resolve")
    public Result<DisputeTicketVO> resolve(@PathVariable Long ticketId,
                                           @Valid @RequestBody DisputeResolveRequest request) {
        return Result.success(disputeTicketService.resolve(
                SecurityContextUtil.currentUserId(),
                ticketId,
                request.getResolution()));
    }
}
