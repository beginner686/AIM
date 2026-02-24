package com.ailink.module.demand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DemandVO {

    private Long id;
    private Long userId;
    private String targetCountry;
    private String category;
    private BigDecimal budget;
    private LocalDateTime deadline;
    private String description;
    private String aiStructured;
    private String status;
    private LocalDateTime createdTime;
}
