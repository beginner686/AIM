package com.ailink.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50, message = "username length must be 3-50")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 20, message = "password length must be 6-20")
    private String password;

    @NotBlank(message = "email is required")
    @Email(message = "email is invalid")
    private String email;

    @Pattern(regexp = "USER|WORKER", message = "role must be USER/WORKER")
    private String role;

    private String country;
    private String city;
}
