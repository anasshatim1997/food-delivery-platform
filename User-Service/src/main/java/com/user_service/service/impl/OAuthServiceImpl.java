package com.user_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.response.AuthResponse;
import com.user_service.dto.response.UserResponse;
import com.user_service.entity.Customer;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.enums.VerificationStatus;
import com.user_service.exception.OAuthException;
import com.user_service.repository.CustomerRepository;
import com.user_service.repository.DriverRepository;
import com.user_service.repository.UserRepository;
import com.user_service.security.JwtService;
import com.user_service.service.IOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements IOAuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${oauth.google.userinfo-url:https://www.googleapis.com/oauth2/v3/userinfo}")
    private String googleUserInfoUrl;

    @Value("${oauth.facebook.userinfo-url:https://graph.facebook.com/me?fields=id,name,email,first_name,last_name}")
    private String facebookUserInfoUrl;

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String accessToken, Role targetRole) {
        OAuthUserInfo userInfo = fetchGoogleUserInfo(accessToken);
        return processOAuthLogin(userInfo, "GOOGLE", targetRole);
    }

    @Override
    @Transactional
    public AuthResponse loginWithFacebook(String accessToken, Role targetRole) {
        OAuthUserInfo userInfo = fetchFacebookUserInfo(accessToken);
        return processOAuthLogin(userInfo, "FACEBOOK", targetRole);
    }

    private OAuthUserInfo fetchGoogleUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    googleUserInfoUrl, HttpMethod.GET, request, String.class
            );

            JsonNode json = objectMapper.readTree(response.getBody());

            return OAuthUserInfo.builder()
                    .providerId(json.get("sub").asText())
                    .email(json.get("email").asText())
                    .firstName(json.path("given_name").asText(""))
                    .lastName(json.path("family_name").asText(""))
                    .profileImage(json.path("picture").asText(null))
                    .emailVerified(json.path("email_verified").asBoolean(false))
                    .build();

        } catch (RestClientException e) {
            log.error("Failed to fetch Google user info: {}", e.getMessage());
            throw new OAuthException("Invalid Google access token");
        } catch (Exception e) {
            log.error("Error parsing Google user info response: {}", e.getMessage());
            throw new OAuthException("Failed to process Google authentication");
        }
    }

    private OAuthUserInfo fetchFacebookUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    facebookUserInfoUrl, HttpMethod.GET, request, String.class
            );

            JsonNode json = objectMapper.readTree(response.getBody());

            if (json.has("error")) {
                throw new OAuthException(
                        "Invalid Facebook access token: " + json.get("error").path("message").asText()
                );
            }

            return OAuthUserInfo.builder()
                    .providerId(json.get("id").asText())
                    .email(json.path("email").asText(null))
                    .firstName(json.path("first_name").asText(""))
                    .lastName(json.path("last_name").asText(""))
                    .profileImage(null)
                    .emailVerified(false)
                    .build();

        } catch (OAuthException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Failed to fetch Facebook user info: {}", e.getMessage());
            throw new OAuthException("Invalid Facebook access token");
        } catch (Exception e) {
            log.error("Error parsing Facebook user info response: {}", e.getMessage());
            throw new OAuthException("Failed to process Facebook authentication");
        }
    }

    private AuthResponse processOAuthLogin(OAuthUserInfo userInfo, String provider, Role targetRole) {
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new OAuthException("Email permission is required. Please grant email access and try again.");
        }

        Optional<User> existingUser = userRepository.findByOauthProviderAndOauthProviderId(
                provider, userInfo.getProviderId()
        );

        User user = existingUser.orElseGet(() -> createUserFromOAuth(userInfo, provider, targetRole));

        if (user.getStatus() == Status.SUSPENDED) {
            throw new OAuthException("Your account has been suspended. Please contact support.");
        }

        boolean isNewUser = existingUser.isEmpty();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .isVerified(user.getIsVerified())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }

    private User createUserFromOAuth(OAuthUserInfo userInfo, String provider, Role targetRole) {
        userRepository.findByEmail(userInfo.getEmail()).ifPresent(existing -> {
            throw new OAuthException(
                    "An account with this email already exists. Please log in with your password instead."
            );
        });

        User user = User.builder()
                .email(userInfo.getEmail())
                .role(targetRole)
                .status(Status.ACTIVE)
                .oauthProvider(provider)
                .oauthProviderId(userInfo.getProviderId())
                .isVerified(userInfo.isEmailVerified())
                .build();

        user = userRepository.save(user);

        if (targetRole == Role.CUSTOMER) {
            createCustomerProfile(user, userInfo);
        } else if (targetRole == Role.DRIVER) {
            createDriverProfile(user, userInfo);
        }

        log.info("New {} user registered via {} OAuth: {}", targetRole, provider, user.getId());
        return user;
    }

    private void createCustomerProfile(User user, OAuthUserInfo userInfo) {
        Customer customer = new Customer();
        customer.setId(user.getId());
        customer.setUser(user);
        customer.setFirstName(userInfo.getFirstName());
        customer.setLastName(userInfo.getLastName());
        customer.setProfileImage(userInfo.getProfileImage());
        customer.setWalletBalance(BigDecimal.ZERO);
        customer.setTotalOrders(0);
        customerRepository.save(customer);
    }

    private void createDriverProfile(User user, OAuthUserInfo userInfo) {
        Driver driver = new Driver();
        driver.setUserId(user.getId());
        driver.setFirstName(userInfo.getFirstName());
        driver.setLastName(userInfo.getLastName());
        driver.setProfileImage(userInfo.getProfileImage());
        driver.setIsAvailable(false);
        driver.setRating(BigDecimal.ZERO);
        driver.setTotalDeliveries(0);
        driver.setWalletBalance(BigDecimal.ZERO);
        driver.setVerificationStatus(VerificationStatus.PENDING);
        driver.setVerificationDocuments(new HashMap<>());
        driverRepository.save(driver);
    }

    @lombok.Builder
    @lombok.Getter
    private static class OAuthUserInfo {
        private final String providerId;
        private final String email;
        private final String firstName;
        private final String lastName;
        private final String profileImage;
        private final boolean emailVerified;
    }
}