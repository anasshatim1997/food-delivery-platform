package com.user_service.dto.response;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String phone;
    private Role role;
    private Status status;
    private Boolean isVerified;
    private String oauthProvider;
    private String oauthProviderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}