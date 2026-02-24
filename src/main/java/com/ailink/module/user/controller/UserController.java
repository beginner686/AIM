package com.ailink.module.user.controller;

import com.ailink.common.Result;
import com.ailink.module.user.dto.UserProfileUpdateRequest;
import com.ailink.module.user.service.UserService;
import com.ailink.module.user.vo.UserVO;
import com.ailink.security.SecurityContextUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<UserVO> currentUser() {
        return Result.success(userService.getCurrentUser(SecurityContextUtil.currentUserId()));
    }

    @PutMapping("/me")
    public Result<Void> updateCurrentUser(@Valid @RequestBody UserProfileUpdateRequest request) {
        userService.updateCurrentUser(SecurityContextUtil.currentUserId(), request);
        return Result.success();
    }
}
