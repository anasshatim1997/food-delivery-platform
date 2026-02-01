package com.user_service.service.impl;

import com.user_service.dto.request.*;
import com.user_service.dto.response.AuthResponse;
import com.user_service.entity.Customer;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.VerificationStatus;
import com.user_service.exception.DuplicateResourceException;
import com.user_service.exception.InvalidOperationException;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.mapper.AuthMapper;
import com.user_service.mapper.UserMapper;
import com.user_service.repository.CustomerRepository;
import com.user_service.repository.DriverRepository;
import com.user_service.repository.UserRepository;
import com.user_service.security.JwtService;
import com.user_service.service.IAuthService;
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
    private final AuthMapper authMapper;
    private final UserMapper userMapper;



    @Override
    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request) {
        validateUniqueUser(request.getEmail(), request.getPhone());

        User user = userMapper.toUser(request, passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setWalletBalance(BigDecimal.ZERO);
        customer.setTotalOrders(0);

        customerRepository.save(customer);

        return authMapper.toAuthResponse(user);
    }


    @Override
    @Transactional
    public AuthResponse registerDriver(RegisterRequest request) {
        validateUniqueUser(request.getEmail(), request.getPhone());

        User user = userMapper.toUser(request, passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.DRIVER);
        userRepository.save(user);

        Driver driver = new Driver();
        driver.setUserId(user.getId());
        driver.setFirstName(request.getFirstName());
        driver.setLastName(request.getLastName());
        driver.setVehicleType(request.getVehicleType());
        driver.setVehicleNumber(request.getVehicleNumber());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setVerificationStatus(VerificationStatus.PENDING);
        driver.setVerificationDocuments(request.getVerificationDocuments());
        driver.setIsAvailable(false);
        driver.setRating(BigDecimal.ZERO);
        driver.setTotalDeliveries(0);
        driver.setWalletBalance(BigDecimal.ZERO);
        driverRepository.save(driver);

        return authMapper.toAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = getUserByEmail(request.getEmail());
        return authMapper.toAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidOperationException("Invalid refresh token");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = getUserByEmail(email);

        return authMapper.toAuthResponse(user, refreshToken);
    }

    @Override
    @SuppressWarnings("unused")
    public AuthResponse oauthLogin(OAuthLoginRequest request) {
        throw new UnsupportedOperationException("OAuth login not implemented yet");
    }

    @Override
    @SuppressWarnings("unused")
    public void verifyEmail(String token) {
        throw new UnsupportedOperationException("Email verification not implemented yet");
    }

    @Override
    @SuppressWarnings("unused")
    public void resendVerificationEmail(String email) {
        throw new UnsupportedOperationException("Resend verification email not implemented yet");
    }

    private void validateUniqueUser(String email, String phone) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("User", "email", email);
        }
        if (phone != null && userRepository.findByPhone(phone).isPresent()) {
            throw new DuplicateResourceException("User", "phone", phone);
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}