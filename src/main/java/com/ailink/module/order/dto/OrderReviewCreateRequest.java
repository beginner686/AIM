package com.ailink.module.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderReviewCreateRequest {

    @NotNull(message = "score is required")
    @Min(value = 1, message = "score must be >= 1")
    @Max(value = 5, message = "score must be <= 5")
    private Integer score;

    private String content;
}
