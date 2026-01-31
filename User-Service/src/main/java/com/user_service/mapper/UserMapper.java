package com.user_service.mapper;

import com.user_service.dto.request.RegisterRequest;
import com.user_service.dto.response.UserResponse;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId() != null ? user.getId().toString() : null)
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .isVerified(user.getIsVerified())
                .build();
    }

    public User toUser(RegisterRequest request, String encodedPassword) {
        if (request == null) {
            return null;
        }

        return User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .phone(request.getPhone())
                .role(Role.valueOf(request.getRole()))
                .status(Status.ACTIVE)
                .isVerified(false)
                .build();
    }
}