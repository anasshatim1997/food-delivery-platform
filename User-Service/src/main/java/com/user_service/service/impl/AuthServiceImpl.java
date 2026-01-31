package com.user_service.service.impl;

import com.user_service.dto.request.*;
import com.user_service.dto.response.AuthResponse;
import com.user_service.dto.response.OAuthUserInfo;
import com.user_service.entity.Customer;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.enums.VerificationStatus;
import com.user_service.exception.DuplicateResourceException;
import com.user_service.exception.InvalidOperationException;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.mapper.AuthMapper;
import com.user_service.repository.CustomerRepository;
import com.user_service.repository.DriverRepository;
import com.user_service.repository.UserRepository;
import com.user_service.security.CustomUserDetails;
import com.user_service.security.JwtService;
import com.user_service.service.IAuthService;
import com.user_service.service.IOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final IOAuthService oauthService;
    private final AuthMapper authMapper;

    @Override
    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request) {
        log.info("Registering new customer with email: {}", request.getEmail());

        validateUniqueUser(request.getEmail(), request.getPhone());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .build();

        userRepository.save(user);

        Customer customer = new Customer();
        customer.setId(user.getId());
        customer.setEmail(user.getEmail());
        customer.setPassword(user.getPassword());
        customer.setPhone(user.getPhone());
        customer.setRole(user.getRole());
        customer.setStatus(user.getStatus());
        customer.setIsVerified(user.getIsVerified());
        customer.setWalletBalance(BigDecimal.ZERO);
        customer.setTotalOrders(0);

        customerRepository.save(customer);

        log.info("Customer registered successfully: {}", request.getEmail());

        return authMapper.toAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse registerDriver(RegisterRequest request) {
        log.info("Registering new driver with email: {}", request.getEmail());

        validateUniqueUser(request.getEmail(), request.getPhone());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.DRIVER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .build();

        userRepository.save(user);

        Driver driver = new Driver();
        driver.setId(user.getId());
        driver.setEmail(user.getEmail());
        driver.setPassword(user.getPassword());
        driver.setPhone(user.getPhone());
        driver.setRole(user.getRole());
        driver.setStatus(user.getStatus());
        driver.setIsVerified(user.getIsVerified());
        driver.setAvailable(false);
        driver.setRating(BigDecimal.ZERO);
        driver.setTotalDeliveries(0);
        driver.setWalletBalance(BigDecimal.ZERO);
        driver.setVerificationStatus(VerificationStatus.PENDING);

        driverRepository.save(driver);

        log.info("Driver registered successfully: {}", request.getEmail());

        return authMapper.toAuthResponse(user);
    }

    @Override
    @Transactional
    public void updateCustomerProfile(String email, CreateCustomerRequest request) {
        log.info("Updating customer profile for: {}", email);

        User user = getUserByEmail(email);

        if (!user.getRole().equals(Role.CUSTOMER)) {
            throw new InvalidOperationException("User is not a customer");
        }

        Customer customer = customerRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", user.getId().toString()));

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        if (request.getProfileImage() != null) {
            customer.setProfileImage(request.getProfileImage());
        }

        customerRepository.save(customer);

        log.info("Customer profile updated: {}", email);
    }

    @Override
    @Transactional
    public void updateDriverProfile(String email, CreateDriverRequest request) {
        log.info("Updating driver profile for: {}", email);

        User user = getUserByEmail(email);

        if (!user.getRole().equals(Role.DRIVER)) {
            throw new InvalidOperationException("User is not a driver");
        }

        Driver driver = driverRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", user.getId().toString()));

        driver.setVehicleType(request.getVehicleType());
        driver.setVehicleNumber(request.getVehicleNumber());
        driver.setLicenseNumber(request.getLicenseNumber());

        driverRepository.save(driver);

        log.info("Driver profile updated: {}", email);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = getUserByEmail(request.getEmail());

        log.info("User logged in successfully: {}", user.getEmail());

        return authMapper.toAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refresh token request received");

        String refreshToken = request.getRefreshToken();

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidOperationException("Provided token is not a refresh token");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = getUserByEmail(email);

        CustomUserDetails userDetails = CustomUserDetails.fromUser(user);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new InvalidOperationException("Invalid or expired refresh token");
        }

        log.info("Refresh token validated successfully for user: {}", email);

        return authMapper.toAuthResponse(user, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse oauthLogin(OAuthLoginRequest request) {
        log.info("OAuth login attempt with provider: {}", request.getProvider());

        OAuthUserInfo oauthUserInfo = verifyOAuthToken(request.getProvider(), request.getProviderToken());

        User user = userRepository.findByEmail(oauthUserInfo.getEmail())
                .orElseGet(() -> createOAuthUser(oauthUserInfo));

        if (user.getOauthProvider() == null) {
            user.setOauthProvider(oauthUserInfo.getProvider());
            user.setOauthProviderId(oauthUserInfo.getProviderId());
            userRepository.save(user);
        }

        log.info("OAuth login successful for: {}", user.getEmail());

        return authMapper.toAuthResponse(user);
    }

    private OAuthUserInfo verifyOAuthToken(String provider, String token) {
        return switch (provider.toUpperCase()) {
            case "GOOGLE" -> oauthService.verifyGoogleToken(token);
            case "FACEBOOK" -> oauthService.verifyFacebookToken(token);
            default -> throw new InvalidOperationException("Unsupported OAuth provider: " + provider);
        };
    }

    private User createOAuthUser(OAuthUserInfo oauthUserInfo) {
        log.info("Creating new OAuth user: {}", oauthUserInfo.getEmail());

        User user = User.builder()
                .email(oauthUserInfo.getEmail())
                .password(null)
                .phone(null)
                .role(Role.USER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .oauthProvider(oauthUserInfo.getProvider())
                .oauthProviderId(oauthUserInfo.getProviderId())
                .build();

        return userRepository.save(user);
    }

    private void validateUniqueUser(String email, String phone) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("User", "email", email);
        }

        if (userRepository.findByPhone(phone).isPresent()) {
            throw new DuplicateResourceException("User", "phone", phone);
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}