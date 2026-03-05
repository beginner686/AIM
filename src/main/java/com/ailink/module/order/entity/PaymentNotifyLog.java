package com.ailink.module.order.entity;

import com.ailink.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_notify_log")
public class PaymentNotifyLog extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventId;

    private String paymentNo;

    private String transactionId;

    private String notifyContent;

    private String processStatus;

    private String errorMsg;
}
