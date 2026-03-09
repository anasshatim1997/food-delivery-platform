package com.user_service.service.impl;

import com.user_service.dto.response.DriverProfileResponse;
import com.user_service.entity.Driver;
import com.user_service.enums.DocumentType;
import com.user_service.enums.VerificationStatus;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.mapper.DriverMapper;
import com.user_service.repository.DriverRepository;
import com.user_service.service.IDriverService;
import com.user_service.service.IStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements IDriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;
    private final IStorageService storageService;

    @Override
    @Transactional
    public void updateLocation(UUID driverId, BigDecimal latitude, BigDecimal longitude) {
        Driver driver = findDriverOrThrow(driverId);

        driver.setCurrentLat(latitude);
        driver.setCurrentLng(longitude);
        driverRepository.save(driver);

        log.info("Updated location for driver {}: ({}, {})", driverId, latitude, longitude);
    }

    @Override
    @Transactional
    public DriverProfileResponse toggleAvailability(UUID driverId, boolean isAvailable) {
        Driver driver = findDriverOrThrow(driverId);

        if (isAvailable && driver.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new IllegalStateException("Driver must be verified before going online");
        }

        driver.setIsAvailable(isAvailable);
        driverRepository.save(driver);

        log.info("Driver {} availability set to {}", driverId, isAvailable);

        return driverMapper.toDriverProfileResponse(driver);
    }

    @Override
    @Transactional
    public String uploadVerificationDocument(UUID driverId, DocumentType documentType, MultipartFile file) {
        Driver driver = findDriverOrThrow(driverId);

        String documentUrl = storageService.uploadFile(file, "driver-documents");

        Map<String, String> documents = driver.getVerificationDocuments();
        if (documents == null) {
            documents = new HashMap<>();
        }
        documents.put(documentType.name(), documentUrl);
        driver.setVerificationDocuments(documents);

        driverRepository.save(driver);

        log.info("Uploaded {} document for driver {}", documentType, driverId);

        return documentUrl;
    }

    private Driver findDriverOrThrow(UUID driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
    }
}