package com.ailink.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiDemandStructRequest {

    @NotBlank(message = "description is required")
    private String description;

    private String targetCountry;
    private String category;
}
