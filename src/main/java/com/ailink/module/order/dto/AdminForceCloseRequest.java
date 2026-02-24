package com.ailink.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminForceCloseRequest {

    @NotBlank(message = "reason is required")
    private String reason;
}
