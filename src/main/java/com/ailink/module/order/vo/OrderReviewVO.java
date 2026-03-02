package com.ailink.module.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderReviewVO {

    private Long id;
    private Long orderId;
    private Long reviewerId;
    private Long revieweeId;
    private Integer score;
    private String content;
    private LocalDateTime createdTime;
}
