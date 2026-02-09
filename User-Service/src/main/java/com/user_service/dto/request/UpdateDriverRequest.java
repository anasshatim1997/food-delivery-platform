package com.user_service.dto.request;
import com.user_service.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDriverRequest {
    private String firstName;

    private String lastName;

    private VehicleType vehicleType;

    private String vehicleNumber;

    private String licenseNumber;

    private Boolean isAvailable;
}