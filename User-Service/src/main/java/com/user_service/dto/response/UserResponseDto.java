package com.user_service.dto.response;

import com.user_service.enums.Role;
import com.user_service.enums.Status;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserResponseDto {

    private UUID id;
    private String email;
    private String phoneNumber;
    private Role role;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
