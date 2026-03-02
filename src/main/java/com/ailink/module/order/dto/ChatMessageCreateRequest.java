package com.ailink.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessageCreateRequest {

    @NotBlank(message = "content is required")
    private String content;
}
