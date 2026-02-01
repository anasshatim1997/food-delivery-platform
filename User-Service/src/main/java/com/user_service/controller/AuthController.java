package com.user_service.controller;

import com.user_service.dto.request.LoginRequest;
import com.user_service.dto.request.OAuthLoginRequest;
import com.user_service.dto.request.RefreshTokenRequest;
import com.user_service.dto.request.RegisterRequest;
import com.user_service.dto.response.ApiResponse;
import com.user_service.dto.response.AuthResponse;
import com.user_service.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

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
}