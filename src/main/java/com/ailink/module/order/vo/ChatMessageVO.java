package com.ailink.module.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVO {

    private Long id;
    private Long orderId;
    private Long senderId;
    private String content;
    private Boolean warning;
    private String warningMessage;
    private LocalDateTime createdTime;
}
