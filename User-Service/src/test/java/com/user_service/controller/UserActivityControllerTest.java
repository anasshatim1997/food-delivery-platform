package com.user_service.controller;

import com.user_service.dto.response.UserActivityResponse;
import com.user_service.service.IUserActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivityControllerTest {

    @Mock
    private IUserActivityService userActivityService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserActivityController userActivityController;

    private UUID userId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);
        when(authentication.getName()).thenReturn(userId.toString());
    }

    @Test
    void getActivityLog_returnsPageOfActivities() {
        UserActivityResponse activity = new UserActivityResponse();
        Page<UserActivityResponse> page = new PageImpl<>(List.of(activity));
        when(userActivityService.getActivityLog(userId, pageable)).thenReturn(page);

        ResponseEntity<Page<UserActivityResponse>> response = userActivityController.getActivityLog(authentication, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(userActivityService).getActivityLog(userId, pageable);
    }

    @Test
    void getActivityLog_returnsEmptyPage_whenNoActivities() {
        Page<UserActivityResponse> emptyPage = new PageImpl<>(List.of());
        when(userActivityService.getActivityLog(userId, pageable)).thenReturn(emptyPage);

        ResponseEntity<Page<UserActivityResponse>> response = userActivityController.getActivityLog(authentication, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void getLoginHistory_returnsPageOfLoginActivities() {
        UserActivityResponse loginActivity = new UserActivityResponse();
        Page<UserActivityResponse> page = new PageImpl<>(List.of(loginActivity));
        when(userActivityService.getLoginHistory(userId, pageable)).thenReturn(page);

        ResponseEntity<Page<UserActivityResponse>> response = userActivityController.getLoginHistory(authentication, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(userActivityService).getLoginHistory(userId, pageable);
    }

    @Test
    void getLoginHistory_returnsEmptyPage_whenNoLogins() {
        Page<UserActivityResponse> emptyPage = new PageImpl<>(List.of());
        when(userActivityService.getLoginHistory(userId, pageable)).thenReturn(emptyPage);

        ResponseEntity<Page<UserActivityResponse>> response = userActivityController.getLoginHistory(authentication, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void getActivityLog_parsesUserIdFromAuthentication() {
        Page<UserActivityResponse> page = new PageImpl<>(List.of());
        when(userActivityService.getActivityLog(userId, pageable)).thenReturn(page);

        userActivityController.getActivityLog(authentication, pageable);

        verify(authentication).getName();
        verify(userActivityService).getActivityLog(userId, pageable);
    }

    @Test
    void getLoginHistory_parsesUserIdFromAuthentication() {
        Page<UserActivityResponse> page = new PageImpl<>(List.of());
        when(userActivityService.getLoginHistory(userId, pageable)).thenReturn(page);

        userActivityController.getLoginHistory(authentication, pageable);

        verify(authentication).getName();
        verify(userActivityService).getLoginHistory(userId, pageable);
    }

    @Test
    void getActivityLog_returnsMultipleActivities() {
        List<UserActivityResponse> activities = List.of(
                new UserActivityResponse(),
                new UserActivityResponse(),
                new UserActivityResponse()
        );
        Page<UserActivityResponse> page = new PageImpl<>(activities);
        when(userActivityService.getActivityLog(userId, pageable)).thenReturn(page);

        ResponseEntity<Page<UserActivityResponse>> response = userActivityController.getActivityLog(authentication, pageable);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(3);
    }
}