package com.user_service.controller;

import com.user_service.dto.response.UserActivityResponse;
import com.user_service.security.AppSecurityConfig;
import com.user_service.service.IUserActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserActivityController.class)
@Import(AppSecurityConfig.class)
class UserActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @InjectMocks
    private IUserActivityService userActivityService;

    private UserActivityResponse activityResponse;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();

        activityResponse = UserActivityResponse.builder()
                .id(UUID.randomUUID())
                .action("LOGIN")
                .details("User logged in")
                .ipAddress("192.168.1.1")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    class GetActivityLog {

        @Test
        @WithMockUser
        void returnsPagedActivityLogForAuthenticatedUser() throws Exception {
            Page<UserActivityResponse> page = new PageImpl<>(List.of(activityResponse));
            when(userActivityService.getActivityLog(any(UUID.class), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/users/activity"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].action").value("LOGIN"))
                    .andExpect(jsonPath("$.content[0].details").value("User logged in"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockUser
        void returnsEmptyPageWhenNoActivities() throws Exception {
            when(userActivityService.getActivityLog(any(UUID.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/v1/users/activity"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/users/activity"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        void respectsPageableParametersInActivityLog() throws Exception {
            Page<UserActivityResponse> page = new PageImpl<>(List.of());
            when(userActivityService.getActivityLog(any(UUID.class), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/users/activity")
                            .param("page", "1")
                            .param("size", "5"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class GetLoginHistory {

        @Test
        @WithMockUser
        void returnsPagedLoginHistoryForAuthenticatedUser() throws Exception {
            UserActivityResponse loginActivity = UserActivityResponse.builder()
                    .id(UUID.randomUUID())
                    .action("LOGIN")
                    .details("Login success")
                    .ipAddress("10.0.0.1")
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<UserActivityResponse> page = new PageImpl<>(List.of(loginActivity));
            when(userActivityService.getLoginHistory(any(UUID.class), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/users/activity/logins"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].action").value("LOGIN"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockUser
        void returnsEmptyPageWhenNoLoginHistory() throws Exception {
            when(userActivityService.getLoginHistory(any(UUID.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/v1/users/activity/logins"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/users/activity/logins"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        void respectsPageableParametersInLoginHistory() throws Exception {
            Page<UserActivityResponse> page = new PageImpl<>(List.of());
            when(userActivityService.getLoginHistory(any(UUID.class), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/users/activity/logins")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }
    }
}