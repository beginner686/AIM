package com.ailink.module.order.service;

import com.ailink.module.order.vo.DisputeTicketVO;

import java.util.List;

public interface DisputeTicketService {

    DisputeTicketVO create(Long userId, Long orderId, String reason, String evidenceUrl);

    DisputeTicketVO resolve(Long adminId, Long ticketId, String resolution);

    List<DisputeTicketVO> listByOrder(Long userId, Long orderId);
}
