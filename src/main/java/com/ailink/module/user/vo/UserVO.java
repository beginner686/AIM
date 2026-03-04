package com.ailink.module.user.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String role;
    private String username;
    private String email;
    private String country;
    private String city;
    private Integer status;
    private String workerApplyStatus;
    private BigDecimal reviewScore;
    private String verifyStatus;
    private LocalDateTime createdTime;
}
