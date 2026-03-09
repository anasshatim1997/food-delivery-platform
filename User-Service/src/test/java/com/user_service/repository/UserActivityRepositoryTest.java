package com.user_service.repository;

import com.user_service.entity.UserActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserActivityRepositoryTest {

    @Autowired
    private UserActivityRepository userActivityRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userActivityRepository.deleteAll();
    }

    private UserActivity buildActivity(UUID uid, String action, String details, String ip) {
        return UserActivity.builder()
                .userId(uid)
                .action(action)
                .details(details)
                .ipAddress(ip)
                .userAgent("Mozilla/5.0")
                .build();
    }

    @Nested
    class FindByUserIdOrderByCreatedAtDesc {

        @Test
        void returnsActivitiesForCorrectUser() {
            userActivityRepository.save(buildActivity(userId, "LOGIN", "Login success", "10.0.0.1"));
            userActivityRepository.save(buildActivity(userId, "LOGOUT", "Logout", "10.0.0.1"));
            userActivityRepository.save(buildActivity(UUID.randomUUID(), "LOGIN", "Other user", "10.0.0.2"));

            Page<UserActivity> result = userActivityRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            result.getContent().forEach(a -> assertThat(a.getUserId()).isEqualTo(userId));
        }

        @Test
        void returnsEmptyPageWhenUserHasNoActivities() {
            Page<UserActivity> result = userActivityRepository
                    .findByUserIdOrderByCreatedAtDesc(UUID.randomUUID(), PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void returnsResultsInDescendingOrderByCreatedAt() throws InterruptedException {
            UserActivity first = userActivityRepository.save(buildActivity(userId, "LOGIN", "First login", "1.1.1.1"));
            Thread.sleep(10);
            UserActivity second = userActivityRepository.save(buildActivity(userId, "LOGOUT", "Logout", "1.1.1.1"));

            Page<UserActivity> result = userActivityRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

            assertThat(result.getContent().get(0).getId()).isEqualTo(second.getId());
            assertThat(result.getContent().get(1).getId()).isEqualTo(first.getId());
        }

        @Test
        void respectsPaginationForActivityLog() {
            for (int i = 0; i < 6; i++) {
                userActivityRepository.save(buildActivity(userId, "ACTION_" + i, "detail", "1.1.1.1"));
            }

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
            userActivityRepository.save(buildActivity(userId, "LOGIN", "Login", "10.0.0.1"));
            userActivityRepository.save(buildActivity(userId, "LOGIN", "Login again", "10.0.0.2"));
            userActivityRepository.save(buildActivity(userId, "LOGOUT", "Logout", "10.0.0.1"));

            List<UserActivity> result = userActivityRepository
                    .findByUserIdAndAction(userId, "LOGIN", PageRequest.of(0, 10));

            assertThat(result).hasSize(2);
            result.forEach(a -> assertThat(a.getAction()).isEqualTo("LOGIN"));
        }

        @Test
        void returnsEmptyListWhenActionNotFound() {
            userActivityRepository.save(buildActivity(userId, "LOGIN", "Login", "10.0.0.1"));

            List<UserActivity> result = userActivityRepository
                    .findByUserIdAndAction(userId, "PASSWORD_CHANGE", PageRequest.of(0, 10));

            assertThat(result).isEmpty();
        }

        @Test
        void doesNotReturnActivitiesFromOtherUsers() {
            UUID otherUserId = UUID.randomUUID();
            userActivityRepository.save(buildActivity(otherUserId, "LOGIN", "Other user login", "1.2.3.4"));
            userActivityRepository.save(buildActivity(userId, "LOGIN", "My login", "5.6.7.8"));

            List<UserActivity> result = userActivityRepository
                    .findByUserIdAndAction(userId, "LOGIN", PageRequest.of(0, 10));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getUserId()).isEqualTo(userId);
        }

        @Test
        void respectsPaginationInFindByUserIdAndAction() {
            for (int i = 0; i < 5; i++) {
                userActivityRepository.save(buildActivity(userId, "LOGIN", "Login " + i, "1.1.1.1"));
            }

            List<UserActivity> result = userActivityRepository
                    .findByUserIdAndAction(userId, "LOGIN", PageRequest.of(0, 2));

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class CountByUserIdAndAction {

        @Test
        void returnsCorrectCountForUserAndAction() {
            userActivityRepository.save(buildActivity(userId, "LOGIN", "Login 1", "1.1.1.1"));
            userActivityRepository.save(buildActivity(userId, "LOGIN", "Login 2", "1.1.1.2"));
            userActivityRepository.save(buildActivity(userId, "LOGOUT", "Logout", "1.1.1.1"));

            long count = userActivityRepository.countByUserIdAndAction(userId, "LOGIN");

            assertThat(count).isEqualTo(2);
        }

        @Test
        void returnsZeroWhenNoMatchingActivities() {
            long count = userActivityRepository.countByUserIdAndAction(userId, "LOGIN");

            assertThat(count).isZero();
        }

        @Test
        void doesNotCountActivitiesFromOtherUsers() {
            userActivityRepository.save(buildActivity(UUID.randomUUID(), "LOGIN", "Other", "1.1.1.1"));

            long count = userActivityRepository.countByUserIdAndAction(userId, "LOGIN");

            assertThat(count).isZero();
        }
    }

    @Nested
    class FindRecentActivities {

        @Test
        void returnsActivitiesAfterGivenTimestamp() throws InterruptedException {
            userActivityRepository.save(buildActivity(userId, "LOGIN", "Old login", "1.1.1.1"));
            Thread.sleep(20);
            LocalDateTime since = LocalDateTime.now();
            Thread.sleep(10);
            userActivityRepository.save(buildActivity(userId, "LOGIN", "Recent login", "1.1.1.1"));

            List<UserActivity> result = userActivityRepository.findRecentActivities(userId, since);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getDetails()).isEqualTo("Recent login");
        }

        @Test
        void returnsEmptyListWhenNoRecentActivities() {
            LocalDateTime future = LocalDateTime.now().plusHours(1);

            List<UserActivity> result = userActivityRepository.findRecentActivities(userId, future);

            assertThat(result).isEmpty();
        }

        @Test
        void doesNotReturnActivitiesFromOtherUsers() {
            UUID otherUserId = UUID.randomUUID();
            LocalDateTime since = LocalDateTime.now().minusMinutes(1);

            userActivityRepository.save(buildActivity(otherUserId, "LOGIN", "Other user", "2.2.2.2"));
            userActivityRepository.save(buildActivity(userId, "LOGIN", "My activity", "1.1.1.1"));

            List<UserActivity> result = userActivityRepository.findRecentActivities(userId, since);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getUserId()).isEqualTo(userId);
        }

        @Test
        void returnsResultsInDescendingOrder() throws InterruptedException {
            LocalDateTime since = LocalDateTime.now().minusMinutes(1);

            UserActivity first = userActivityRepository.save(buildActivity(userId, "LOGIN", "First", "1.1.1.1"));
            Thread.sleep(10);
            UserActivity second = userActivityRepository.save(buildActivity(userId, "LOGOUT", "Second", "1.1.1.1"));

            List<UserActivity> result = userActivityRepository.findRecentActivities(userId, since);

            assertThat(result.get(0).getId()).isEqualTo(second.getId());
            assertThat(result.get(1).getId()).isEqualTo(first.getId());
        }
    }
}