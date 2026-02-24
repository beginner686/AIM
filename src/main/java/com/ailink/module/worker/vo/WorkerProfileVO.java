package com.ailink.module.worker.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WorkerProfileVO {

    private Long id;
    private Long userId;
    private String country;
    private String city;
    private String skillTags;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private String experience;
    private Float rating;
    private Integer verified;
    private String realName;
    private LocalDateTime createdTime;
}
