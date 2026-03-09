package com.user_service.service;

import com.user_service.dto.response.UserActivityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IUserActivityService {

    void logActivity(UUID userId, String action, String details, String ipAddress, String userAgent);

    Page<UserActivityResponse> getActivityLog(UUID userId, Pageable pageable);

    Page<UserActivityResponse> getLoginHistory(UUID userId, Pageable pageable);
}