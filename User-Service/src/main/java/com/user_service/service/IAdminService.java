package com.user_service.service;

import com.user_service.dto.request.UpdateStatusRequest;
import com.user_service.dto.request.UpdateVerificationRequest;
import com.user_service.dto.response.UserResponse;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IAdminService {

    Page<UserResponse> searchUsers(String email, Role role, Status status, Pageable pageable);

    UserResponse getUserById(UUID userId);

    void updateUserStatus(UUID userId, UpdateStatusRequest request);

    void updateDriverVerification(UUID driverId, UpdateVerificationRequest request);
}