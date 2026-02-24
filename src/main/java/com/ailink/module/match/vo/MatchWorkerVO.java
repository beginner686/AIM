package com.ailink.module.match.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MatchWorkerVO {

    private Long workerProfileId;
    private Long workerUserId;
    private String country;
    private String city;
    private String skillTags;
    private Float rating;
    private Integer verified;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private BigDecimal matchScore;
}
