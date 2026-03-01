package com.user_service.dto.request;
import com.user_service.enums.VehicleType;
import com.user_service.validation.annotation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;
import java.util.Map;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    @Pattern(regexp = "^(\\+212|0)([67])\\d{8}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private VehicleType vehicleType;

    private String vehicleNumber;

    private String licenseNumber;

    private Map<@NotBlank String, @NotBlank @URL String> verificationDocuments;
}