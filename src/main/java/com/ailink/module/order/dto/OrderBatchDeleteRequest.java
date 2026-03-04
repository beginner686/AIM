package com.ailink.module.order.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderBatchDeleteRequest {

    @NotEmpty(message = "orderIds is required")
    private List<Long> orderIds;
}
