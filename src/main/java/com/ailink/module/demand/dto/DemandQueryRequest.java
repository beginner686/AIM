package com.ailink.module.demand.dto;

import lombok.Data;

@Data
public class DemandQueryRequest {

    private String targetCountry;
    private String category;
    private String status;
}
