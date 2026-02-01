package com.user_service.dto.request;

import com.user_service.enums.VehicleType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    private String phone;

    private String firstName;
    private String lastName;
    private String profileImage;

    private VehicleType vehicleType;
    private String vehicleNumber;
}
