package com.ailink.module.order.entity;

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
@TableName("orders")
public class Order extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long demandId;
    private Long employerId;
    private Long workerProfileId;
    private Long workerUserId;
    private BigDecimal amount;
    private String status;
    private String serviceFeeStatus;
    private BigDecimal serviceFeeAmount;
    private LocalDateTime serviceFeePaidTime;
    private String payStatus;
    private String paymentChannel;
    private BigDecimal platformFeeRate;
    private BigDecimal platformFee;
    private BigDecimal escrowFeeRate;
    private BigDecimal escrowFee;
    private BigDecimal workerIncome;
    private String riskStatus;
    private String disputeReason;
    private String closedReason;
}
