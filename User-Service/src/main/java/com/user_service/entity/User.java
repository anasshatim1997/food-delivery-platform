package com.user_service.entity;

import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.validation.annotation.StrongPassword;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Pattern(regexp = "^(\\+212|0)([67])\\d{8}$")
    @Column(unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private Role role = Role.USER;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVerified = false;

    private String verificationCode;

    private LocalDateTime verificationCodeExpiresAt;

    private String oauthProvider;
    private String oauthProviderId;

    @Builder.Default
    @Column(nullable = false)
    private boolean profileCompleted = false;

    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiresAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
