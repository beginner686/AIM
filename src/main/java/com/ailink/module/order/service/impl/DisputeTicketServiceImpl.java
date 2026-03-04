package com.ailink.module.order.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.enums.OrderStatus;
import com.ailink.common.exception.BizException;
import com.ailink.module.ai.service.AiService;
import com.ailink.module.order.entity.DisputeTicket;
import com.ailink.module.order.entity.Order;
import com.ailink.module.order.mapper.DisputeTicketMapper;
import com.ailink.module.order.mapper.OrderMapper;
import com.ailink.module.order.service.DisputeTicketService;
import com.ailink.module.order.service.OrderService;
import com.ailink.module.order.vo.DisputeTicketVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class DisputeTicketServiceImpl implements DisputeTicketService {

    private final DisputeTicketMapper disputeTicketMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final AiService aiService;

    public DisputeTicketServiceImpl(DisputeTicketMapper disputeTicketMapper,
            OrderMapper orderMapper,
            OrderService orderService,
            AiService aiService) {
        this.disputeTicketMapper = disputeTicketMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.aiService = aiService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisputeTicketVO create(Long userId, Long orderId, String reason, String evidenceUrl) {
        Order order = findOrder(orderId);
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only order participants can raise dispute");
        }
        if (!OrderStatus.MATCH_UNLOCKED.name().equals(order.getStatus())
                && !OrderStatus.IN_PROGRESS.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "dispute can only be raised after match unlock");
        }

        DisputeTicket ticket = new DisputeTicket();
        ticket.setOrderId(orderId);
        ticket.setCreatorId(userId);
        ticket.setReason(reason);
        ticket.setEvidenceUrl(evidenceUrl);
        ticket.setStatus("OPEN");
        disputeTicketMapper.insert(ticket);

        orderService.updateOrderStatus(userId, orderId, OrderStatus.DISPUTE, "dispute raised");

        // 异步获取 AI 仲裁分析
        CompletableFuture.runAsync(() -> {
            try {
                String report = aiService.generateDisputeArbitrationReport(orderId);
                DisputeTicket update = new DisputeTicket();
                update.setId(ticket.getId());
                update.setAiAnalysisReport(report);
                disputeTicketMapper.updateById(update);
            } catch (Exception e) {
                // ignore
            }
        });

        return toVO(ticket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisputeTicketVO resolve(Long adminId, Long ticketId, String resolution) {
        DisputeTicket ticket = disputeTicketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "dispute ticket not found");
        }
        if (!"OPEN".equalsIgnoreCase(ticket.getStatus())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "dispute ticket already resolved");
        }

        ticket.setStatus("RESOLVED");
        ticket.setResolverId(adminId);
        ticket.setResolution(resolution);
        ticket.setResolvedTime(LocalDateTime.now());
        disputeTicketMapper.updateById(ticket);

        Order order = findOrder(ticket.getOrderId());
        if (OrderStatus.DISPUTE.name().equals(order.getStatus())) {
            orderService.updateOrderStatus(adminId, order.getId(), OrderStatus.ARBITRATION,
                    "dispute resolved by admin");
        }
        return toVO(ticket);
    }

    @Override
    public List<DisputeTicketVO> listByOrder(Long userId, Long orderId) {
        Order order = findOrder(orderId);
        if (!userId.equals(order.getEmployerId()) && !userId.equals(order.getWorkerUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "only order participants can view dispute tickets");
        }
        return disputeTicketMapper.selectList(new LambdaQueryWrapper<DisputeTicket>()
                .eq(DisputeTicket::getOrderId, orderId)
                .orderByDesc(DisputeTicket::getCreatedTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private Order findOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "order not found");
        }
        return order;
    }

    private DisputeTicketVO toVO(DisputeTicket ticket) {
        DisputeTicketVO vo = new DisputeTicketVO();
        vo.setId(ticket.getId());
        vo.setOrderId(ticket.getOrderId());
        vo.setCreatorId(ticket.getCreatorId());
        vo.setReason(ticket.getReason());
        vo.setEvidenceUrl(ticket.getEvidenceUrl());
        vo.setStatus(StringUtils.hasText(ticket.getStatus()) ? ticket.getStatus() : "OPEN");
        vo.setResolverId(ticket.getResolverId());
        vo.setResolution(ticket.getResolution());
        vo.setResolvedTime(ticket.getResolvedTime());
        vo.setAiAnalysisReport(ticket.getAiAnalysisReport());
        vo.setCreatedTime(ticket.getCreatedTime());
        return vo;
    }
}
