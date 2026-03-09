package com.user_service.service.impl;

import com.user_service.dto.response.UserActivityResponse;
import com.user_service.entity.UserActivity;
import com.user_service.repository.UserActivityRepository;
import com.user_service.service.IUserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityServiceImpl implements IUserActivityService {

    private final UserActivityRepository userActivityRepository;

    @Override
    @Async
    @Transactional
    public void logActivity(UUID userId, String action, String details, String ipAddress, String userAgent) {
        UserActivity activity = UserActivity.builder()
                .userId(userId)
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        userActivityRepository.save(activity);
        log.debug("Logged activity for user {}: {}", userId, action);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserActivityResponse> getActivityLog(UUID userId, Pageable pageable) {
        return userActivityRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserActivityResponse> getLoginHistory(UUID userId, Pageable pageable) {
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        List<UserActivity> activities = userActivityRepository.findByUserIdAndAction(userId, "LOGIN", pageRequest);
        long total = userActivityRepository.countByUserIdAndAction(userId, "LOGIN");

        return new PageImpl<>(activities, pageable, total)
                .map(this::toResponse);
    }

    private UserActivityResponse toResponse(UserActivity activity) {
        return UserActivityResponse.builder()
                .id(activity.getId())
                .action(activity.getAction())
                .details(activity.getDetails())
                .ipAddress(activity.getIpAddress())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}