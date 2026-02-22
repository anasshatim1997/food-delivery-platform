package com.user_service.service.impl;

import com.user_service.dto.request.*;
import com.user_service.dto.response.AuthResponse;
import com.user_service.dto.response.UserResponse;
import com.user_service.entity.Customer;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.enums.VerificationStatus;
import com.user_service.exception.OAuthException;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.repository.CustomerRepository;
import com.user_service.repository.DriverRepository;
import com.user_service.repository.UserRepository;
import com.user_service.security.JwtService;
import com.user_service.service.IAuthService;
import com.user_service.service.IOAuthService;
import com.user_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final StorageService storageService;
    private final IOAuthService oAuthService;

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterUserRequest request) {
        assertEmailNotTaken(request.getEmail());
        assertPhoneNotTaken(request.getPhone());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .isVerified(false)
                .build();

        userRepository.save(user);
        log.info("User registered: {} role={}", user.getId(), user.getRole());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request) {
        User user = createAndSaveUser(request, Role.CUSTOMER);

        Customer customer = new Customer();
        customer.setId(user.getId());
        customer.setUser(user);
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setWalletBalance(BigDecimal.ZERO);
        customer.setTotalOrders(0);
        customerRepository.save(customer);

        log.info("Customer registered: {}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse registerDriver(RegisterRequest request) {
        User user = createAndSaveUser(request, Role.DRIVER);

        Driver driver = new Driver();
        driver.setUserId(user.getId());
        driver.setFirstName(request.getFirstName());
        driver.setLastName(request.getLastName());
        driver.setVehicleType(request.getVehicleType());
        driver.setVehicleNumber(request.getVehicleNumber());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setIsAvailable(false);
        driver.setRating(BigDecimal.ZERO);
        driver.setTotalDeliveries(0);
        driver.setWalletBalance(BigDecimal.ZERO);
        driver.setVerificationStatus(VerificationStatus.PENDING);
        driver.setVerificationDocuments(new HashMap<>());
        driverRepository.save(driver);

        log.info("Driver registered: {}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse completeCustomerProfile(UUID userId, CompleteCustomerProfileRequest request) {
        User user = findUserOrThrow(userId);
        Customer customer = findCustomerOrThrow(userId);

        customer.setProfileImage(uploadIfPresent(request.getProfileImage(), "customers/profiles"));
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customerRepository.save(customer);

        log.info("Customer profile completed: {}", userId);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse completeDriverProfile(UUID userId, CompleteDriverProfileRequest request) {
        User user = findUserOrThrow(userId);
        Driver driver = findDriverByUserIdOrThrow(userId);

        driver.setProfileImage(uploadIfPresent(request.getProfileImage(), "drivers/profiles"));
        driver.setLicenseImage(uploadIfPresent(request.getLicenseImage(), "drivers/licenses"));
        driver.setFirstName(request.getFirstName());
        driver.setLastName(request.getLastName());
        driver.setVehicleType(request.getVehicleType());
        driver.setVehicleNumber(request.getVehicleNumber());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setVerificationStatus(VerificationStatus.PENDING);
        driverRepository.save(driver);

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

        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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

        user.setVerificationCode(UUID.randomUUID().toString());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        log.info("Verification email queued for: {}", email);
    }

    private User createAndSaveUser(RegisterRequest request, Role role) {
        assertEmailNotTaken(request.getEmail());
        assertPhoneNotTaken(request.getPhone());

        return userRepository.save(User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .status(Status.ACTIVE)
                .isVerified(false)
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
                        .oauthProvider(user.getOauthProvider())
                        .oauthProviderId(user.getOauthProviderId())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .build();
    }

    private String uploadIfPresent(MultipartFile file, String path) {
        return (file != null && !file.isEmpty()) ? storageService.uploadFile(file, path) : null;
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

    private Customer findCustomerOrThrow(UUID userId) {
        return customerRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + userId));
    }

    private Driver findDriverByUserIdOrThrow(UUID userId) {
        return driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + userId));
    }
}