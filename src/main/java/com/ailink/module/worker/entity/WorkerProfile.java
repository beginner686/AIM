package com.ailink.module.worker.entity;

import com.ailink.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("worker_profile")
public class WorkerProfile extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String country;
    private String city;
    private String skillTags;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private String experience;
    private Float rating;
    private Integer verified;
    private String realName;
    private String idNoHash;
}
