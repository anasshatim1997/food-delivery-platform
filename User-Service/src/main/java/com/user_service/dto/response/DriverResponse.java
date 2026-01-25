package com.user_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponse {

    private String id;

    private String email;

    private String phone;

    private String vehicleType;

    private String vehicleNumber;

    private String licenseNumber;

    private Boolean isAvailable;

    private BigDecimal currentLat;

    private BigDecimal currentLng;

    private BigDecimal rating;

    private Integer totalDeliveries;

    private BigDecimal walletBalance;

    private String verificationStatus;

    private Map<String, String> verificationDocuments;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}