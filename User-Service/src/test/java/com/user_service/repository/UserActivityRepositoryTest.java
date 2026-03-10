package com.user_service.repository;

import com.user_service.entity.UserActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivityRepositoryTest {

    @Mock
    private UserActivityRepository userActivityRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private UserActivity buildActivity(UUID uid, String action, String details) {
        UserActivity activity = new UserActivity();
        activity.setId(UUID.randomUUID());
        activity.setUserId(uid);
        activity.setAction(action);
        activity.setDetails(details);
        activity.setIpAddress("10.0.0.1");
        return activity;
    }

    @Nested
    class FindByUserIdOrderByCreatedAtDesc {

        @Test
        void returnsActivitiesForCorrectUser() {
            UserActivity a1 = buildActivity(userId, "LOGIN", "Login success");
            UserActivity a2 = buildActivity(userId, "LOGOUT", "Logout");
            Page<UserActivity> page = new PageImpl<>(List.of(a1, a2));
            when(userActivityRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10)))
                    .thenReturn(page);

            Page<UserActivity> result = userActivityRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            result.getContent().forEach(a -> assertThat(a.getUserId()).isEqualTo(userId));
        }

        @Test
        void returnsEmptyPageWhenUserHasNoActivities() {
            UUID otherId = UUID.randomUUID();
            Page<UserActivity> emptyPage = new PageImpl<>(List.of());
            when(userActivityRepository.findByUserIdOrderByCreatedAtDesc(otherId, PageRequest.of(0, 10)))
                    .thenReturn(emptyPage);

            Page<UserActivity> result = userActivityRepository
                    .findByUserIdOrderByCreatedAtDesc(otherId, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void returnsResultsInDescendingOrderByCreatedAt() {
            UserActivity first = buildActivity(userId, "LOGIN", "First login");
            UserActivity second = buildActivity(userId, "LOGOUT", "Logout");
            Page<UserActivity> page = new PageImpl<>(List.of(second, first));
            when(userActivityRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10)))
                    .thenReturn(page);

            Page<UserActivity> result = userActivityRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

            assertThat(result.getContent().get(0).getId()).isEqualTo(second.getId());
            assertThat(result.getContent().get(1).getId()).isEqualTo(first.getId());
        }

        @Test
        void respectsPaginationForActivityLog() {
            List<UserActivity> activities = List.of(
                    buildActivity(userId, "A1", "d"),
                    buildActivity(userId, "A2", "d"),
                    buildActivity(userId, "A3", "d")
            );
            Page<UserActivity> page = new PageImpl<>(activities, PageRequest.of(0, 3), 6);
            when(userActivityRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 3)))
                    .thenReturn(page);

            Page<UserActivity> result = userActivityRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 3));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(6);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }
    }

    @Nested
    class FindByUserIdAndAction {

        @Test
        void returnsOnlyActivitiesMatchingAction() {
            UserActivity a1 = buildActivity(userId, "LOGIN", "Login");
            UserActivity a2 = buildActivity(userId, "LOGIN", "Login again");
            when(userActivityRepository.findByUserIdAndAction(userId, "LOGIN", PageRequest.of(0, 10)))
                    .thenReturn(List.of(a1, a2));

            List<UserActivity> result = userActivityRepository
                    .findByUserIdAndAction(userId, "LOGIN", PageRequest.of(0, 10));

            assertThat(result).hasSize(2);
            result.forEach(a -> assertThat(a.getAction()).isEqualTo("LOGIN"));
        }

        @Test
        void returnsEmptyListWhenActionNotFound() {
            when(userActivityRepository.findByUserIdAndAction(userId, "PASSWORD_CHANGE", PageRequest.of(0, 10)))
                    .thenReturn(List.of());

            List<UserActivity> result = userActivityRepository
                    .findByUserIdAndAction(userId, "PASSWORD_CHANGE", PageRequest.of(0, 10));

            assertThat(result).isEmpty();
        }

        @Test
        void doesNotReturnActivitiesFromOtherUsers() {
            UserActivity mine = buildActivity(userId, "LOGIN", "My login");
            when(userActivityRepository.findByUserIdAndAction(userId, "LOGIN", PageRequest.of(0, 10)))
                    .thenReturn(List.of(mine));

            List<UserActivity> result = userActivityRepository
                    .findByUserIdAndAction(userId, "LOGIN", PageRequest.of(0, 10));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getUserId()).isEqualTo(userId);
        }

        @Test
        void respectsPaginationInFindByUserIdAndAction() {
            List<UserActivity> twoResults = List.of(
                    buildActivity(userId, "LOGIN", "Login 1"),
                    buildActivity(userId, "LOGIN", "Login 2")
            );
            when(userActivityRepository.findByUserIdAndAction(userId, "LOGIN", PageRequest.of(0, 2)))
                    .thenReturn(twoResults);

            List<UserActivity> result = userActivityRepository
                    .findByUserIdAndAction(userId, "LOGIN", PageRequest.of(0, 2));

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class CountByUserIdAndAction {

        @Test
        void returnsCorrectCountForUserAndAction() {
            when(userActivityRepository.countByUserIdAndAction(userId, "LOGIN")).thenReturn(2L);

            long count = userActivityRepository.countByUserIdAndAction(userId, "LOGIN");

            assertThat(count).isEqualTo(2);
        }

        @Test
        void returnsZeroWhenNoMatchingActivities() {
            when(userActivityRepository.countByUserIdAndAction(userId, "LOGIN")).thenReturn(0L);

            long count = userActivityRepository.countByUserIdAndAction(userId, "LOGIN");

            assertThat(count).isZero();
        }

        @Test
        void doesNotCountActivitiesFromOtherUsers() {
            when(userActivityRepository.countByUserIdAndAction(userId, "LOGIN")).thenReturn(0L);

            long count = userActivityRepository.countByUserIdAndAction(userId, "LOGIN");

            assertThat(count).isZero();
            verify(userActivityRepository).countByUserIdAndAction(userId, "LOGIN");
        }
    }

    @Nested
    class FindRecentActivities {

        @Test
        void returnsActivitiesAfterGivenTimestamp() {
            LocalDateTime since = LocalDateTime.now().minusMinutes(5);
            UserActivity recent = buildActivity(userId, "LOGIN", "Recent login");
            when(userActivityRepository.findRecentActivities(userId, since))
                    .thenReturn(List.of(recent));

            List<UserActivity> result = userActivityRepository.findRecentActivities(userId, since);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getDetails()).isEqualTo("Recent login");
        }

        @Test
        void returnsEmptyListWhenNoRecentActivities() {
            LocalDateTime future = LocalDateTime.now().plusHours(1);
            when(userActivityRepository.findRecentActivities(userId, future)).thenReturn(List.of());

            List<UserActivity> result = userActivityRepository.findRecentActivities(userId, future);

            assertThat(result).isEmpty();
        }

        @Test
        void doesNotReturnActivitiesFromOtherUsers() {
            LocalDateTime since = LocalDateTime.now().minusMinutes(1);
            UserActivity mine = buildActivity(userId, "LOGIN", "My activity");
            when(userActivityRepository.findRecentActivities(userId, since)).thenReturn(List.of(mine));

            List<UserActivity> result = userActivityRepository.findRecentActivities(userId, since);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getUserId()).isEqualTo(userId);
        }

        @Test
        void returnsResultsInDescendingOrder() {
            LocalDateTime since = LocalDateTime.now().minusMinutes(1);
            UserActivity first = buildActivity(userId, "LOGIN", "First");
            UserActivity second = buildActivity(userId, "LOGOUT", "Second");
            when(userActivityRepository.findRecentActivities(userId, since))
                    .thenReturn(List.of(second, first));

            List<UserActivity> result = userActivityRepository.findRecentActivities(userId, since);

            assertThat(result.get(0).getId()).isEqualTo(second.getId());
            assertThat(result.get(1).getId()).isEqualTo(first.getId());
        }
    }
}