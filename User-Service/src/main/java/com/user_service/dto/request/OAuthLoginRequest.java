package com.user_service.dto.request;

import com.user_service.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginRequest {

    @NotBlank(message = "Provider is required")
    private String provider;

    @NotBlank(message = "Access token is required")
    private String accessToken;

    @NotNull(message = "Target role is required")
    private Role targetRole;
}