package com.user_service.dto.request;

import com.user_service.validation.annotation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetPasswordRequest {
    @NotBlank
    @StrongPassword
    private String password;
}