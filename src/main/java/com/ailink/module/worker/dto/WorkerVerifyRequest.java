package com.ailink.module.worker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkerVerifyRequest {

    @NotNull(message = "verified is required")
    @Min(value = 0, message = "verified must be 0 or 1")
    @Max(value = 1, message = "verified must be 0 or 1")
    private Integer verified;
}
