package com.ailink.module.escrow.service;

import com.ailink.module.escrow.vo.EscrowRecordVO;

import java.math.BigDecimal;

public interface EscrowService {

    void hold(Long orderId, BigDecimal amount, String channel);

    void release(Long orderId);

    void refund(Long orderId);

    EscrowRecordVO getByOrderId(Long orderId);
}
