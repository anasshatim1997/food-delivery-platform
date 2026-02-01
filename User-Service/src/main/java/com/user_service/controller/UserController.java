package com.user_service.controller;

import com.user_service.dto.request.UpdateCustomerRequest;
import com.user_service.dto.request.UpdateDriverRequest;
import com.user_service.dto.response.ApiResponse;
import com.user_service.dto.response.UserProfileResponse;
import com.user_service.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved"));
    }

    @PatchMapping("/me/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCustomerProfile(
            @Valid @RequestBody UpdateCustomerRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserProfileResponse updatedProfile = userService.updateCustomerProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Customer profile updated"));
    }

    @PatchMapping("/me/driver")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateDriverProfile(
            @Valid @RequestBody UpdateDriverRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserProfileResponse updatedProfile = userService.updateDriverProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Driver profile updated"));
    }

    @PostMapping("/me/profile-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> uploadProfileImage(
            @RequestPart MultipartFile image,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        String imageUrl = userService.uploadProfileImage(userId, image);
        return ResponseEntity.ok(ApiResponse.success(imageUrl, "Profile image uploaded"));
    }
}