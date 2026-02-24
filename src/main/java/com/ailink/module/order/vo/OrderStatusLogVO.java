package com.ailink.module.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderStatusLogVO {

    private Long id;
    private Long orderId;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdTime;
}
