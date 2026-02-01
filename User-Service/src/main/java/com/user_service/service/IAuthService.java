package com.user_service.service;

import com.user_service.dto.request.LoginRequest;
import com.user_service.dto.request.OAuthLoginRequest;
import com.user_service.dto.request.RefreshTokenRequest;
import com.user_service.dto.request.RegisterRequest;
import com.user_service.dto.response.AuthResponse;

public interface IAuthService {

    AuthResponse registerCustomer(RegisterRequest request);

    AuthResponse registerDriver(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    AuthResponse oauthLogin(OAuthLoginRequest request);

    void verifyEmail(String token);

    void resendVerificationEmail(String email);
}