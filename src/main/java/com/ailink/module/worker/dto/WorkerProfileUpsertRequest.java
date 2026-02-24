package com.ailink.module.worker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerProfileUpsertRequest {

    @NotBlank(message = "country is required")
    private String country;

    @NotBlank(message = "city is required")
    private String city;

    @NotBlank(message = "skillTags is required")
    private String skillTags;

    @DecimalMin(value = "0", message = "priceMin must be >= 0")
    private BigDecimal priceMin;

    @DecimalMin(value = "0", message = "priceMax must be >= 0")
    private BigDecimal priceMax;

    private String experience;

    @NotBlank(message = "realName is required")
    private String realName;

    @NotBlank(message = "idNoHash is required")
    private String idNoHash;
}
