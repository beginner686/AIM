package com.ailink.module.worker.entity;

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
@TableName("worker_apply")
public class WorkerApply extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String status;
    private String country;
    private String city;
    private String skillTags;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private String experience;
    private String realName;
    private String idNoHash;
    private String applyNote;
    private String reviewNote;
    private Long reviewedBy;
    private LocalDateTime reviewedTime;
}
