package com.ailink.module.worker.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkerApplyReviewRequest {

    @Size(max = 500, message = "reviewNote length must be <= 500")
    private String reviewNote;
}
