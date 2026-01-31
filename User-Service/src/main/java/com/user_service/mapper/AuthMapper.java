package com.user_service.mapper;

import com.user_service.dto.response.AuthResponse;
import com.user_service.dto.response.UserResponse;
import com.user_service.entity.User;
import com.user_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthResponse toAuthResponse(User user) {
        return toAuthResponse(user, jwtService.generateRefreshToken(user));
    }

    public AuthResponse toAuthResponse(User user, String refreshToken) {
        UserResponse userResponse = userMapper.toUserResponse(user);

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(userResponse)
                .build();
    }
}