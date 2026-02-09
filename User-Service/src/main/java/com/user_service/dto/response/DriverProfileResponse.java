package com.user_service.dto.response;
import com.user_service.enums.VehicleType;
import com.user_service.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.util.Map;
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DriverProfileResponse extends UserProfileResponse {
    private String firstName;
    private String lastName;
    private String profileImage;
    private VehicleType vehicleType;
    private String vehicleNumber;
    private String licenseNumber;
    private Boolean isAvailable;
    private BigDecimal currentLat;
    private BigDecimal currentLng;
    private BigDecimal rating;
    private Integer totalDeliveries;
    private BigDecimal walletBalance;
    private VerificationStatus verificationStatus;
    private Map<String, String> verificationDocuments;
}