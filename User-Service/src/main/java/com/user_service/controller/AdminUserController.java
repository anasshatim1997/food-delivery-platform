package com.user_service.controller;

import com.user_service.dto.request.UpdateStatusRequest;
import com.user_service.dto.request.UpdateVerificationRequest;
import com.user_service.dto.response.UserResponse;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.security.RoleAnnotations;
import com.user_service.service.IAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final IAdminService adminService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Status status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.searchUsers(email, role, status, pageable));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateStatusRequest request) {
        adminService.updateUserStatus(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/drivers/{driverId}/verification")
    public ResponseEntity<Void> updateDriverVerification(
            @PathVariable UUID driverId,
            @Valid @RequestBody UpdateVerificationRequest request) {
        adminService.updateDriverVerification(driverId, request);
        return ResponseEntity.noContent().build();
    }
}