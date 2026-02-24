package com.ailink.module.user.service;

import com.ailink.module.user.dto.LoginRequest;
import com.ailink.module.user.dto.RegisterRequest;
import com.ailink.module.user.dto.UserProfileUpdateRequest;
import com.ailink.module.user.vo.LoginVO;
import com.ailink.module.user.vo.UserVO;

public interface UserService {

    Long register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    UserVO getCurrentUser(Long userId);

    void updateCurrentUser(Long userId, UserProfileUpdateRequest request);
}
