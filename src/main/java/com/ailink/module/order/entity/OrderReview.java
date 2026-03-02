package com.ailink.module.order.entity;

import com.ailink.common.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_review")
public class OrderReview extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long reviewerId;
    private Long revieweeId;
    private Integer score;
    private String content;
}
