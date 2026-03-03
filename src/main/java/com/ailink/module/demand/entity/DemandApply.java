package com.ailink.module.demand.entity;

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
@TableName("demand_apply")
public class DemandApply extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long demandId;
    private Long demandOwnerId;
    private Long workerUserId;
    private Long workerProfileId;
    private BigDecimal quoteAmount;
    private String applyNote;
    private String status;
    private String reviewNote;
    private Long orderId;
    private LocalDateTime handledTime;
}

