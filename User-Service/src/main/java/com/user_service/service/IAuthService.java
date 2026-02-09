package com.user_service.service;

import com.user_service.dto.request.*;
import com.user_service.dto.response.AuthResponse;

import java.util.UUID;

public interface IAuthService {

    AuthResponse registerUser(RegisterUserRequest request);

    AuthResponse completeCustomerProfile(UUID userId, CompleteCustomerProfileRequest request);

    AuthResponse completeDriverProfile(UUID userId, CompleteDriverProfileRequest request);

    AuthResponse registerCustomer(RegisterRequest request);

    AuthResponse registerDriver(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    AuthResponse oauthLogin(OAuthLoginRequest request);

    void verifyEmail(String token);

    void resendVerificationEmail(String email);
}