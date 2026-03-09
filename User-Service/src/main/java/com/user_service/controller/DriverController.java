package com.user_service.controller;

import com.user_service.dto.request.ToggleAvailabilityRequest;
import com.user_service.dto.response.DriverProfileResponse;
import com.user_service.entity.Driver;
import com.user_service.enums.DocumentType;
import com.user_service.repository.DriverRepository;
import com.user_service.security.RoleAnnotations;
import com.user_service.service.IDriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final IDriverService driverService;
    private final DriverRepository driverRepository;

    @PatchMapping("/me/location")
    public ResponseEntity<Void> updateLocation(
            Authentication authentication,
            @Valid @RequestBody com.AtlasEats.UserService.dto.request.UpdateLocationRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        driverService.updateLocation(driver.getId(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/availability")
    public ResponseEntity<DriverProfileResponse> toggleAvailability(
            Authentication authentication,
            @Valid @RequestBody ToggleAvailabilityRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        DriverProfileResponse response = driverService.toggleAvailability(driver.getId(), request.getIsAvailable());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/documents/{documentType}")
    public ResponseEntity<Map<String, String>> uploadDocument(
            Authentication authentication,
            @PathVariable DocumentType documentType,
            @RequestParam("file") MultipartFile file) {
        UUID userId = UUID.fromString(authentication.getName());
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        String documentUrl = driverService.uploadVerificationDocument(driver.getId(), documentType, file);
        return ResponseEntity.ok(Map.of("documentUrl", documentUrl));
    }
}