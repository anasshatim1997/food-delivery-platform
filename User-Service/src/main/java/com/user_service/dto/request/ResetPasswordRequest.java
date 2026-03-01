package com.user_service.dto.request;

import com.user_service.validation.annotation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String token;

    @NotBlank
    @StrongPassword
    private String newPassword;
}