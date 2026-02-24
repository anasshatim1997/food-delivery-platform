package com.user_service.service.impl;

import com.user_service.dto.request.*;
import com.user_service.dto.response.AuthResponse;
import com.user_service.dto.response.UserResponse;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.exception.OAuthException;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.repository.UserRepository;
import com.user_service.security.JwtService;
import com.user_service.service.IAuthService;
import com.user_service.service.IEmailService;
import com.user_service.service.IOAuthService;
import com.user_service.service.IUserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final IOAuthService oAuthService;
    private final IEmailService emailService;
    private final IUserProfileService userProfileService;

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterUserRequest request) {
        assertEmailNotTaken(request.getEmail());
        assertPhoneNotTaken(request.getPhone());

        String verificationCode = UUID.randomUUID().toString();

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .isVerified(false)
                .profileCompleted(false)
                .verificationCode(verificationCode)
                .verificationCodeExpiresAt(LocalDateTime.now().plusHours(24))
                .build();

        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);
        log.info("User registered: {} role={}", user.getId(), user.getRole());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request) {
        User user = createAndSaveUser(request, Role.CUSTOMER);
        userProfileService.createCustomerProfile(user, request.getFirstName(), request.getLastName());
        user.setProfileCompleted(true);
        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationCode());
        emailService.sendWelcomeEmail(user.getEmail(), request.getFirstName());
        log.info("Customer registered: {}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse registerDriver(RegisterRequest request) {
        User user = createAndSaveUser(request, Role.DRIVER);
        userProfileService.createDriverProfile(user, request.getFirstName(), request.getLastName(),
                String.valueOf(request.getVehicleType()), request.getVehicleNumber(), request.getLicenseNumber());
        user.setProfileCompleted(true);
        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationCode());
        emailService.sendWelcomeEmail(user.getEmail(), request.getFirstName());
        log.info("Driver registered: {}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse completeCustomerProfile(UUID userId, CompleteCustomerProfileRequest request) {
        User user = findUserOrThrow(userId);
        userProfileService.completeCustomerProfile(userId, user, request);
        user.setProfileCompleted(true);
        userRepository.save(user);
        emailService.sendWelcomeEmail(user.getEmail(), request.getFirstName());
        log.info("Customer profile completed: {}", userId);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse completeDriverProfile(UUID userId, CompleteDriverProfileRequest request) {
        User user = findUserOrThrow(userId);
        userProfileService.completeDriverProfile(userId, user, request);
        user.setProfileCompleted(true);
        userRepository.save(user);
        emailService.sendWelcomeEmail(user.getEmail(), request.getFirstName());
        log.info("Driver profile completed: {}", userId);
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getStatus() == Status.SUSPENDED) {
            throw new IllegalStateException("Your account has been suspended. Please contact support.");
        }
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtService.isRefreshToken(token)) {
            throw new IllegalArgumentException("Provided token is not a refresh token");
        }
        if (jwtService.isTokenExpired(token)) {
            throw new IllegalArgumentException("Refresh token has expired. Please log in again.");
        }
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getStatus() == Status.SUSPENDED) {
            throw new IllegalStateException("Your account has been suspended. Please contact support.");
        }
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse oauthLogin(OAuthLoginRequest request) {
        return switch (request.getProvider().toUpperCase()) {
            case "GOOGLE" -> oAuthService.loginWithGoogle(request.getAccessToken(), request.getTargetRole());
            case "FACEBOOK" -> oAuthService.loginWithFacebook(request.getAccessToken(), request.getTargetRole());
            default -> throw new OAuthException("Unsupported OAuth provider: " + request.getProvider());
        };
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationCode(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code"));
        if (user.getVerificationCodeExpiresAt() != null
                && user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired. Please request a new one.");
        }
        user.setIsVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
        log.info("Email verified for user: {}", user.getId());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + email));
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalStateException("Email is already verified");
        }
        String verificationCode = UUID.randomUUID().toString();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);
        emailService.sendVerificationEmail(email, verificationCode);
        log.info("Verification email resent to: {}", email);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + email));
        if (user.getOauthProvider() != null && user.getPassword() == null) {
            throw new IllegalStateException(
                    "This account uses " + user.getOauthProvider() + " login. Use OAuth to sign in or set a password first."
            );
        }
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        emailService.sendPasswordResetEmail(email, resetToken);
        log.info("Password reset email sent to: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token"));
        if (user.getPasswordResetTokenExpiresAt() == null
                || user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired. Please request a new one.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
        emailService.sendPasswordChangedNotification(user.getEmail());
        log.info("Password reset for user: {}", user.getId());
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);
        if (user.getPassword() == null) {
            throw new IllegalStateException("No password is set for this account. Use 'Set Password' to add one.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        emailService.sendPasswordChangedNotification(user.getEmail());
        log.info("Password changed for user: {}", userId);
    }

    @Override
    @Transactional
    public AuthResponse linkOAuthProvider(UUID userId, OAuthLoginRequest request) {
        User user = findUserOrThrow(userId);
        String provider = request.getProvider().toUpperCase();
        if (provider.equals(user.getOauthProvider())) {
            throw new IllegalStateException(provider + " is already linked to this account.");
        }
        AuthResponse oauthResponse = switch (provider) {
            case "GOOGLE" -> oAuthService.loginWithGoogle(request.getAccessToken(), user.getRole());
            case "FACEBOOK" -> oAuthService.loginWithFacebook(request.getAccessToken(), user.getRole());
            default -> throw new OAuthException("Unsupported OAuth provider: " + request.getProvider());
        };
        String oauthEmail = oauthResponse.getUser().getEmail();
        if (!oauthEmail.equalsIgnoreCase(user.getEmail())) {
            throw new OAuthException("The " + provider + " account email does not match your registered email.");
        }
        userRepository.findByOauthProviderAndOauthProviderId(provider, oauthResponse.getUser().getOauthProviderId())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(userId)) {
                        throw new OAuthException("This " + provider + " account is already linked to another user.");
                    }
                });
        user.setOauthProvider(provider);
        user.setOauthProviderId(oauthResponse.getUser().getOauthProviderId());
        userRepository.save(user);
        log.info("Linked {} OAuth to user: {}", provider, userId);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse setPasswordForOAuthUser(UUID userId, SetPasswordRequest request) {
        User user = findUserOrThrow(userId);
        if (user.getPassword() != null) {
            throw new IllegalStateException("A password is already set. Use 'Change Password' to update it.");
        }
        if (user.getOauthProvider() == null) {
            throw new IllegalStateException("This endpoint is only for OAuth users who have no password set.");
        }
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        emailService.sendPasswordChangedNotification(user.getEmail());
        log.info("Password set for OAuth user: {}", userId);
        return buildAuthResponse(user);
    }

    private User createAndSaveUser(RegisterRequest request, Role role) {
        assertEmailNotTaken(request.getEmail());
        assertPhoneNotTaken(request.getPhone());
        String verificationCode = UUID.randomUUID().toString();
        return userRepository.save(User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .status(Status.ACTIVE)
                .isVerified(false)
                .profileCompleted(false)
                .verificationCode(verificationCode)
                .verificationCodeExpiresAt(LocalDateTime.now().plusHours(24))
                .build());
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .user(UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .isVerified(user.getIsVerified())
                        .profileCompleted(user.isProfileCompleted())
                        .oauthProvider(user.getOauthProvider())
                        .oauthProviderId(user.getOauthProviderId())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .build();
    }

    private void assertEmailNotTaken(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
    }

    private void assertPhoneNotTaken(String phone) {
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Phone number is already registered");
        }
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}