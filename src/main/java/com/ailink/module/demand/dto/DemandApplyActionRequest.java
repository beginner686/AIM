package com.ailink.module.demand.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DemandApplyActionRequest {

    private BigDecimal amount;
    private String reviewNote;
}

