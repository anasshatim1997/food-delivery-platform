package com.user_service.controller;

import com.user_service.dto.request.CreateCustomerRequest;
import com.user_service.dto.request.CreateDriverRequest;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<AuthResponse>> registerCustomer(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.registerCustomer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Customer registered successfully"));
    }

    @PostMapping("/register/driver")
    public ResponseEntity<ApiResponse<AuthResponse>> registerDriver(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.registerDriver(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Driver registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Login successful")
        );
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Token refreshed successfully")
        );
    }

    @PostMapping("/oauth/login")
    public ResponseEntity<ApiResponse<AuthResponse>> oauthLogin(
            @Valid @RequestBody OAuthLoginRequest request
    ) {
        AuthResponse response = authService.oauthLogin(request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "OAuth login successful")
        );
    }

    @PutMapping("/customer/profile")
    public ResponseEntity<ApiResponse<Void>> updateCustomerProfile(
            Authentication authentication,
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        authService.updateCustomerProfile(authentication.getName(), request);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Customer profile updated successfully")
        );
    }

    @PutMapping("/driver/profile")
    public ResponseEntity<ApiResponse<Void>> updateDriverProfile(
            Authentication authentication,
            @Valid @RequestBody CreateDriverRequest request
    ) {
        authService.updateDriverProfile(authentication.getName(), request);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Driver profile updated successfully")
        );
    }
}
