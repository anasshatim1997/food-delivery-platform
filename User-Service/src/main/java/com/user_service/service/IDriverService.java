package com.user_service.service;


import com.user_service.dto.response.DriverProfileResponse;
import com.user_service.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

public interface IDriverService {

    void updateLocation(UUID driverId, BigDecimal latitude, BigDecimal longitude);

    DriverProfileResponse toggleAvailability(UUID driverId, boolean isAvailable);

    String uploadVerificationDocument(UUID driverId, DocumentType documentType, MultipartFile file);
}