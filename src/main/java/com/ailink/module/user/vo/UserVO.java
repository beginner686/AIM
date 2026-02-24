package com.ailink.module.user.vo;

import lombok.Data;

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
    private LocalDateTime createdTime;
}
