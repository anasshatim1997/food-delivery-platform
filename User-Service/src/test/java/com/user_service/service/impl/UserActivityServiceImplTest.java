package com.user_service.service.impl;


import com.user_service.dto.response.UserActivityResponse;
import com.user_service.entity.UserActivity;
import com.user_service.repository.UserActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserActivityService Tests")
class UserActivityServiceImplTest {

    @Mock
    private UserActivityRepository userActivityRepository;

    @InjectMocks
    private UserActivityServiceImpl userActivityService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("logActivity Tests")
    class LogActivityTests {

        @Test
        @DisplayName("Should log activity successfully")
        void logActivity_success() {
            when(userActivityRepository.save(any(UserActivity.class))).thenAnswer(i -> i.getArgument(0));

            userActivityService.logActivity(userId, "LOGIN", "User logged in", "127.0.0.1", "Mozilla/5.0");

            ArgumentCaptor<UserActivity> captor = ArgumentCaptor.forClass(UserActivity.class);
            verify(userActivityRepository).save(captor.capture());

            UserActivity saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getAction()).isEqualTo("LOGIN");
            assertThat(saved.getDetails()).isEqualTo("User logged in");
            assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        }
    }

    @Nested
    @DisplayName("getActivityLog Tests")
    class GetActivityLogTests {

        @Test
        @DisplayName("Should return paginated activity log")
        void getActivityLog_success() {
            UserActivity activity = UserActivity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .action("PROFILE_UPDATE")
                    .details("Updated profile picture")
                    .ipAddress("127.0.0.1")
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<UserActivity> page = new PageImpl<>(List.of(activity));
            Pageable pageable = PageRequest.of(0, 20);

            when(userActivityRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                    .thenReturn(page);

            Page<UserActivityResponse> result = userActivityService.getActivityLog(userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAction()).isEqualTo("PROFILE_UPDATE");
        }
    }

    @Nested
    @DisplayName("getLoginHistory Tests")
    class GetLoginHistoryTests {

        @Test
        @DisplayName("Should return login history")
        void getLoginHistory_success() {
            UserActivity loginActivity = UserActivity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .action("LOGIN")
                    .details("Successful login")
                    .ipAddress("192.168.1.1")
                    .createdAt(LocalDateTime.now())
                    .build();

            Pageable pageable = PageRequest.of(0, 20);

            when(userActivityRepository.findByUserIdAndAction(eq(userId), eq("LOGIN"), any(Pageable.class)))
                    .thenReturn(List.of(loginActivity));

            Page<UserActivityResponse> result = userActivityService.getLoginHistory(userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent().get(0).getAction()).isEqualTo("LOGIN");
        }
    }
}