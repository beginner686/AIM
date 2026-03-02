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
@TableName("platform_payment")
public class PlatformPayment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;
    private Long orderId;
    private Long payerId;
    private String paymentChannel;
    private BigDecimal amount;
    private String status;
    private String transactionId;
    private String transactionNo;
    private String codeUrl;
    private LocalDateTime paidTime;
    private String remark;
}
