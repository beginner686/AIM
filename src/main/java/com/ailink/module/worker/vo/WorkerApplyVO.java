package com.ailink.module.worker.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WorkerApplyVO {

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
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
