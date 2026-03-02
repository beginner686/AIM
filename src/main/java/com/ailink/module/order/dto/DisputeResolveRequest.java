package com.ailink.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DisputeResolveRequest {

    @NotBlank(message = "resolution is required")
    private String resolution;
}
