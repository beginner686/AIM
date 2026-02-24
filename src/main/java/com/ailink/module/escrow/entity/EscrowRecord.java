package com.ailink.module.escrow.entity;

import com.ailink.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("escrow_record")
public class EscrowRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private BigDecimal amount;
    private String channel;
    private String status;
    private String transactionNo;
    private LocalDateTime releasedTime;
}
