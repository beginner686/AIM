package com.ailink.module.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCreateRequest {

    @NotNull(message = "demandId is required")
    private Long demandId;

    @NotNull(message = "workerProfileId is required")
    private Long workerProfileId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be > 0")
    private BigDecimal amount;

    private String paymentChannel;
}
