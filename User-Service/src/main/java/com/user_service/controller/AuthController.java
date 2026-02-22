package com.user_service.controller;

import com.user_service.dto.request.*;
import com.user_service.dto.response.ApiResponse;
import com.user_service.dto.response.AuthResponse;
import com.user_service.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(
            @Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        authService.registerUser(request),
                        "User registered successfully. Please complete your profile."));
    }

    @PostMapping("/complete-profile/customer")
    public ResponseEntity<ApiResponse<AuthResponse>> completeCustomerProfile(
            @Valid @RequestBody CompleteCustomerProfileRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success(
                        authService.completeCustomerProfile(userId, request),
                        "Customer profile completed successfully"));
    }

    @PostMapping("/complete-profile/driver")
    public ResponseEntity<ApiResponse<AuthResponse>> completeDriverProfile(
            @Valid @RequestBody CompleteDriverProfileRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success(
                        authService.completeDriverProfile(userId, request),
                        "Driver profile completed successfully"));
    }

    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<AuthResponse>> registerCustomer(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        authService.registerCustomer(request),
                        "Customer registered successfully"));
    }

    @PostMapping("/register/driver")
    public ResponseEntity<ApiResponse<AuthResponse>> registerDriver(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        authService.registerDriver(request),
                        "Driver registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.login(request), "Login successful"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.refreshToken(request), "Token refreshed"));
    }

    @PostMapping("/oauth/login")
    public ResponseEntity<ApiResponse<AuthResponse>> oauthLogin(
            @Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.oauthLogin(request), "OAuth login successful"));
    }

    @PostMapping("/oauth/link")
    public ResponseEntity<ApiResponse<AuthResponse>> linkOAuthProvider(
            @Valid @RequestBody OAuthLoginRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success(
                        authService.linkOAuthProvider(userId, request),
                        "OAuth provider linked successfully"));
    }

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<AuthResponse>> setPassword(
            @Valid @RequestBody SetPasswordRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.success(
                        authService.setPasswordForOAuthUser(userId, request),
                        "Password set successfully"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification email sent"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.success(null,
                "If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }
}