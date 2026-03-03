package com.ailink.module.demand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DemandApplyVO {

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
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    private String workerName;
    private String workerCountry;
    private String workerSkillTags;

    private String demandCategory;
    private String demandCountry;
    private BigDecimal demandBudget;
    private String demandStatus;
}

