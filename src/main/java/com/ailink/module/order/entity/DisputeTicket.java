package com.ailink.module.order.entity;

import com.ailink.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dispute_ticket")
public class DisputeTicket extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long creatorId;
    private String reason;
    private String evidenceUrl;
    private String status;
    private Long resolverId;
    private String resolution;
    private LocalDateTime resolvedTime;
    private String aiAnalysisReport;
}
