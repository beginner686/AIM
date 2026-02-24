package com.ailink.module.user.service.impl;

import com.ailink.common.ErrorCode;
import com.ailink.common.exception.BizException;
import com.ailink.config.JwtProperties;
import com.ailink.module.user.dto.LoginRequest;
import com.ailink.module.user.dto.RegisterRequest;
import com.ailink.module.user.dto.UserProfileUpdateRequest;
import com.ailink.module.user.entity.User;
import com.ailink.module.user.mapper.UserMapper;
import com.ailink.module.user.service.UserService;
import com.ailink.module.user.vo.LoginVO;
import com.ailink.module.user.vo.UserVO;
import com.ailink.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    public UserServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           StringRedisTemplate stringRedisTemplate,
                           JwtProperties jwtProperties) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public Long register(RegisterRequest request) {
        User byUsername = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (byUsername != null) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "username already exists");
        }

        User byEmail = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail()));
        if (byEmail != null) {
            throw new BizException(ErrorCode.BUSINESS_ERROR.getCode(), "email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(StringUtils.hasText(request.getRole()) ? request.getRole() : "USER");
        user.setCountry(request.getCountry());
        user.setCity(request.getCity());
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "username or password incorrect");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "account disabled");
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getRole());
        stringRedisTemplate.opsForValue().set(
                LOGIN_TOKEN_KEY_PREFIX + token,
                "1",
                jwtProperties.getExpireSeconds(),
                TimeUnit.SECONDS);
        return new LoginVO(token, toUserVO(user));
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    public void updateCurrentUser(Long userId, UserProfileUpdateRequest request) {
        User update = new User();
        update.setId(userId);
        update.setEmail(request.getEmail());
        update.setCountry(request.getCountry());
        update.setCity(request.getCity());
        userMapper.updateById(update);
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setRole(user.getRole());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setCountry(user.getCountry());
        vo.setCity(user.getCity());
        vo.setStatus(user.getStatus());
        vo.setCreatedTime(user.getCreatedTime());
        return vo;
    }
}
