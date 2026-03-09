package com.user_service.dto.request;

import com.user_service.entity.Driver;
import com.user_service.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVerificationRequest {

    @NotNull(message = "Verification status is required")
    private VerificationStatus status;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}