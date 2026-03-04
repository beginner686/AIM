package com.ailink.module.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DisputeTicketVO {

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
    private LocalDateTime createdTime;
}
