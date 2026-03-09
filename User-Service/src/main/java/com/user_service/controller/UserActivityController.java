package com.user_service.controller;

import com.user_service.dto.response.UserActivityResponse;
import com.user_service.service.IUserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/activity")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserActivityController {

    private final IUserActivityService userActivityService;

    @GetMapping
    public ResponseEntity<Page<UserActivityResponse>> getActivityLog(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(userActivityService.getActivityLog(userId, pageable));
    }

    @GetMapping("/logins")
    public ResponseEntity<Page<UserActivityResponse>> getLoginHistory(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(userActivityService.getLoginHistory(userId, pageable));
    }
}