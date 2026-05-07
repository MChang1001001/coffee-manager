package com.example.coffeebean.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.coffeebean.common.BusinessException;
import com.example.coffeebean.common.ErrorCode;
import com.example.coffeebean.user.User;
import com.example.coffeebean.user.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username())
                .eq(User::getDeleted, 0)
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername());
        return new LoginResponse(token, toUserProfile(user));
    }

    public UserProfileResponse findCurrentUser(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(currentUser.id());
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return toUserProfile(user);
    }

    private UserProfileResponse toUserProfile(User user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl());
    }
}
