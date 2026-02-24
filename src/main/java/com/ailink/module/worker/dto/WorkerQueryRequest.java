package com.ailink.module.worker.dto;

import lombok.Data;

@Data
public class WorkerQueryRequest {

    private String country;
    private String city;
    private String skillKeyword;
    private Integer verified;
}
