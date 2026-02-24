package com.ailink.module.demand.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DemandCreateRequest {

    @NotBlank(message = "targetCountry is required")
    private String targetCountry;

    @NotBlank(message = "category is required")
    private String category;

    @NotNull(message = "budget is required")
    @DecimalMin(value = "0.01", message = "budget must be greater than 0")
    private BigDecimal budget;

    @Future(message = "deadline must be in future")
    private LocalDateTime deadline;

    @NotBlank(message = "description is required")
    private String description;
}
