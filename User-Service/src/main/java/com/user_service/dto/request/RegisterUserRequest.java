package com.user_service.dto.request;

import com.user_service.enums.Role;
import com.user_service.validation.annotation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {

    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    @Pattern(regexp = "^(\\+212|0)([67])\\d{8}$", message = "Invalid phone number")
    private String phone;
}