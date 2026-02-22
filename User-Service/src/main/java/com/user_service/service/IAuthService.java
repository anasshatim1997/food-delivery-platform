package com.user_service.service;

import com.user_service.dto.request.*;
import com.user_service.dto.response.AuthResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IAuthService {
    AuthResponse registerUser(RegisterUserRequest request);
    AuthResponse registerCustomer(RegisterRequest request);
    AuthResponse registerDriver(RegisterRequest request);
    AuthResponse completeCustomerProfile(UUID userId, CompleteCustomerProfileRequest request);
    AuthResponse completeDriverProfile(UUID userId, CompleteDriverProfileRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    AuthResponse oauthLogin(OAuthLoginRequest request);
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(UUID userId, ChangePasswordRequest request);
    AuthResponse linkOAuthProvider(UUID userId, OAuthLoginRequest request);
    AuthResponse setPasswordForOAuthUser(UUID userId, SetPasswordRequest request);
}