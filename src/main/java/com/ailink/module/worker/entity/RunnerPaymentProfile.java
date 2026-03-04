package com.ailink.module.worker.entity;

import com.ailink.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("runner_payment_profile")
public class RunnerPaymentProfile extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String paypalEmail;
    private String wiseId;
    private String wiseLink;
    private String payoneerLink;
    private String cryptoWallet;
    private String paymentUrl;
    private String currency;
    private Integer verified;
}
