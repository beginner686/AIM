package com.ailink.module.worker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerApplySubmitRequest {

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

    @Size(max = 500, message = "applyNote length must be <= 500")
    private String applyNote;

    @Size(max = 200, message = "applyAttachmentName length must be <= 200")
    private String applyAttachmentName;

    @Size(max = 500, message = "applyAttachmentUrl length must be <= 500")
    private String applyAttachmentUrl;
}
