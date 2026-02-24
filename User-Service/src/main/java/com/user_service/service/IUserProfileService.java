package com.user_service.service;

import com.user_service.dto.request.CompleteCustomerProfileRequest;
import com.user_service.dto.request.CompleteDriverProfileRequest;
import com.user_service.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IUserProfileService {
    void createCustomerProfile(User user, String firstName, String lastName);
    void createDriverProfile(User user, String firstName, String lastName,
                             String vehicleType, String vehicleNumber, String licenseNumber);
    void completeCustomerProfile(UUID userId, User user, CompleteCustomerProfileRequest request);
    void completeDriverProfile(UUID userId, User user, CompleteDriverProfileRequest request);
    String uploadProfileImage(UUID userId, MultipartFile file);
    String uploadDriverProfileImage(UUID userId, MultipartFile file);
    String uploadDriverLicenseImage(UUID userId, MultipartFile file);
}