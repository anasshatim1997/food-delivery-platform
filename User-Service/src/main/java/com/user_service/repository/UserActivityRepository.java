package com.user_service.repository;

import com.user_service.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {

    Page<UserActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT a FROM UserActivity a WHERE a.userId = :userId AND a.action = :action ORDER BY a.createdAt DESC")
    List<UserActivity> findByUserIdAndAction(@Param("userId") UUID userId, @Param("action") String action, Pageable pageable);

    long countByUserIdAndAction(UUID userId, String action);

    @Query("SELECT a FROM UserActivity a WHERE a.userId = :userId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<UserActivity> findRecentActivities(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
}
