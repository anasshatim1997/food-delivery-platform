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
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        UserResponse userResponse = userMapper.toUserResponse(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(userResponse)
                .build();
    }

    public AuthResponse toAuthResponse(User user, String existingRefreshToken) {
        String accessToken = jwtService.generateAccessToken(user);

        UserResponse userResponse = userMapper.toUserResponse(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(existingRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(userResponse)
                .build();
    }
}