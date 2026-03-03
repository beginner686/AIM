package com.ailink.module.demand.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DemandApplySubmitRequest {

    @NotNull(message = "quoteAmount is required")
    @DecimalMin(value = "0.01", message = "quoteAmount must be greater than 0")
    private BigDecimal quoteAmount;

    private String applyNote;
}

