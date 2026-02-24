package com.ailink.module.order.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeConfigUpdateRequest {

    @NotNull(message = "platformFeeRate is required")
    @DecimalMin(value = "0", message = "platformFeeRate must be >= 0")
    @DecimalMax(value = "1", message = "platformFeeRate must be <= 1")
    private BigDecimal platformFeeRate;

    @NotNull(message = "escrowFeeRate is required")
    @DecimalMin(value = "0", message = "escrowFeeRate must be >= 0")
    @DecimalMax(value = "1", message = "escrowFeeRate must be <= 1")
    private BigDecimal escrowFeeRate;
}
