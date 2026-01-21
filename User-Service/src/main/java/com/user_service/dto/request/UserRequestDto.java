package com.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDto {

    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
            message = "Password must contain upper, lower, digit, and special char"
    )
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(\\+212|0)([67])\\d{8}$",
            message = "Invalid Moroccan phone number"
    )
    private String phoneNumber;
}
