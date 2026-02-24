package com.ailink.module.escrow.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.exception.BizException;
import com.ailink.module.escrow.entity.EscrowRecord;
import com.ailink.module.escrow.mapper.EscrowRecordMapper;
import com.ailink.module.escrow.service.EscrowService;
import com.ailink.module.escrow.vo.EscrowRecordVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class EscrowServiceImpl implements EscrowService {

    private final EscrowRecordMapper escrowRecordMapper;

    public EscrowServiceImpl(EscrowRecordMapper escrowRecordMapper) {
        this.escrowRecordMapper = escrowRecordMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hold(Long orderId, BigDecimal amount, String channel) {
        EscrowRecord record = findByOrderId(orderId);
        if (record == null) {
            record = new EscrowRecord();
            record.setOrderId(orderId);
            record.setAmount(amount);
            record.setChannel(StringUtils.hasText(channel) ? channel : "MANUAL");
            record.setStatus("HOLDING");
            record.setTransactionNo("TXN-" + orderId + "-" + System.currentTimeMillis());
            escrowRecordMapper.insert(record);
            return;
        }

        record.setAmount(amount);
        record.setChannel(StringUtils.hasText(channel) ? channel : record.getChannel());
        record.setStatus("HOLDING");
        record.setTransactionNo("TXN-" + orderId + "-" + System.currentTimeMillis());
        record.setReleasedTime(null);
        escrowRecordMapper.updateById(record);
    }

    @Override
    public void release(Long orderId) {
        EscrowRecord record = findByOrderIdOrThrow(orderId);
        record.setStatus("RELEASED");
        record.setReleasedTime(LocalDateTime.now());
        escrowRecordMapper.updateById(record);
    }

    @Override
    public void refund(Long orderId) {
        EscrowRecord record = findByOrderIdOrThrow(orderId);
        record.setStatus("REFUNDED");
        escrowRecordMapper.updateById(record);
    }

    @Override
    public EscrowRecordVO getByOrderId(Long orderId) {
        EscrowRecord record = findByOrderIdOrThrow(orderId);
        EscrowRecordVO vo = new EscrowRecordVO();
        vo.setId(record.getId());
        vo.setOrderId(record.getOrderId());
        vo.setAmount(record.getAmount());
        vo.setChannel(record.getChannel());
        vo.setStatus(record.getStatus());
        vo.setTransactionNo(record.getTransactionNo());
        vo.setReleasedTime(record.getReleasedTime());
        vo.setCreatedTime(record.getCreatedTime());
        return vo;
    }

    private EscrowRecord findByOrderId(Long orderId) {
        return escrowRecordMapper.selectOne(new LambdaQueryWrapper<EscrowRecord>()
                .eq(EscrowRecord::getOrderId, orderId));
    }

    private EscrowRecord findByOrderIdOrThrow(Long orderId) {
        EscrowRecord record = findByOrderId(orderId);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "escrow record not found");
        }
        return record;
    }
}
