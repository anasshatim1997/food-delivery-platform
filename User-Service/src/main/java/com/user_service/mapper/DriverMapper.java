package com.user_service.mapper;

import com.user_service.dto.request.CreateDriverRequest;
import com.user_service.dto.request.UpdateDriverRequest;
import com.user_service.dto.response.DriverResponse;
import com.user_service.entity.Driver;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {

    public DriverResponse toDriverResponse(Driver driver) {
        if (driver == null) {
            return null;
        }

        return DriverResponse.builder()
                .id(driver.getId() != null ? driver.getId().toString() : null)
                .email(driver.getEmail())
                .phone(driver.getPhone())
                .vehicleType(driver.getVehicleType() != null ? driver.getVehicleType().name() : null)
                .vehicleNumber(driver.getVehicleNumber())
                .licenseNumber(driver.getLicenseNumber())
                .isAvailable(driver.isAvailable())
                .currentLat(driver.getCurrentLat())
                .currentLng(driver.getCurrentLng())
                .rating(driver.getRating())
                .totalDeliveries(driver.getTotalDeliveries())
                .walletBalance(driver.getWalletBalance())
                .verificationStatus(driver.getVerificationStatus() != null ? driver.getVerificationStatus().name() : null)
                .verificationDocuments(driver.getVerificationDocuments())
                .status(driver.getStatus() != null ? driver.getStatus().name() : null)
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }

    public Driver toDriver(CreateDriverRequest request) {
        if (request == null) {
            return null;
        }

        Driver driver = new Driver();
        driver.setVehicleType(request.getVehicleType());
        driver.setVehicleNumber(request.getVehicleNumber());
        driver.setLicenseNumber(request.getLicenseNumber());

        return driver;
    }

    public void updateDriver(UpdateDriverRequest request, Driver driver) {
        if (request == null || driver == null) {
            return;
        }

        if (request.getVehicleType() != null) {
            driver.setVehicleType(request.getVehicleType());
        }
        if (request.getVehicleNumber() != null) {
            driver.setVehicleNumber(request.getVehicleNumber());
        }
        if (request.getLicenseNumber() != null) {
            driver.setLicenseNumber(request.getLicenseNumber());
        }
        if (request.getIsAvailable() != null) {
            driver.setAvailable(request.getIsAvailable());
        }
    }
}