package com.user_service.service;

import com.user_service.dto.request.UpdateCustomerRequest;
import com.user_service.dto.request.UpdateDriverRequest;
import com.user_service.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IUserService {
    String uploadProfileImage(UUID userId, MultipartFile image) ;
    UserProfileResponse getProfile(UUID userId);
    UserProfileResponse updateDriverProfile(UUID userId, UpdateDriverRequest request);
    UserProfileResponse updateCustomerProfile(UUID userId, UpdateCustomerRequest request);
}
