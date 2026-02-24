package com.ailink.module.user.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    @Email(message = "email is invalid")
    private String email;

    private String country;
    private String city;
}
