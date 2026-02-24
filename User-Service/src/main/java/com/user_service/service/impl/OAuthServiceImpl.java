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
import com.user_service.service.IEmailService;
import com.user_service.service.IOAuthService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
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
    private final IEmailService emailService;

    @Value("${oauth.google.userinfo-url:https://www.googleapis.com/oauth2/v3/userinfo}")
    private String googleUserInfoUrl;

    @Value("${oauth.google.token-info-url:https://oauth2.googleapis.com/tokeninfo}")
    private String googleTokenInfoUrl;

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.facebook.userinfo-url:https://graph.facebook.com/me?fields=id,name,email,first_name,last_name}")
    private String facebookUserInfoUrl;

    @Value("${oauth.facebook.app-id}")
    private String facebookAppId;

    @Value("${oauth.facebook.app-secret}")
    private String facebookAppSecret;

    @Value("${oauth.facebook.debug-token-url:https://graph.facebook.com/debug_token}")
    private String facebookDebugTokenUrl;

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String accessToken, Role targetRole) {
        validateGoogleToken(accessToken);
        OAuthUserInfo userInfo = fetchGoogleUserInfo(accessToken);
        return processOAuthLogin(userInfo, "GOOGLE", targetRole);
    }

    @Override
    @Transactional
    public AuthResponse loginWithFacebook(String accessToken, Role targetRole) {
        validateFacebookToken(accessToken);
        OAuthUserInfo userInfo = fetchFacebookUserInfo(accessToken);
        return processOAuthLogin(userInfo, "FACEBOOK", targetRole);
    }

    private void validateGoogleToken(String accessToken) {
        try {
            String url = googleTokenInfoUrl + "?access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());

            if (json.has("error")) {
                throw new OAuthException("Invalid Google token: " + json.path("error_description").asText());
            }

            String audience = json.path("aud").asText("");
            if (!googleClientId.equals(audience)) {
                log.warn("Google token audience mismatch. Expected: {}, Got: {}", googleClientId, audience);
                throw new OAuthException("Google token was not issued for this application");
            }

            long expiresIn = json.path("expires_in").asLong(0);
            if (expiresIn <= 0) {
                throw new OAuthException("Google token has expired");
            }

            log.debug("Google token validated successfully for scope: {}", json.path("scope").asText());

        } catch (OAuthException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            throw new OAuthException("Google token validation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error validating Google token: {}", e.getMessage());
            throw new OAuthException("Failed to validate Google token");
        }
    }

    private void validateFacebookToken(String accessToken) {
        try {
            String appToken = facebookAppId + "|" + facebookAppSecret;
            String url = facebookDebugTokenUrl + "?input_token=" + accessToken + "&access_token=" + appToken;

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode data = json.path("data");

            if (!data.path("is_valid").asBoolean(false)) {
                String reason = data.path("error").path("message").asText("Unknown reason");
                throw new OAuthException("Invalid Facebook token: " + reason);
            }

            String tokenAppId = data.path("app_id").asText("");
            if (!facebookAppId.equals(tokenAppId)) {
                log.warn("Facebook token app_id mismatch. Expected: {}, Got: {}", facebookAppId, tokenAppId);
                throw new OAuthException("Facebook token was not issued for this application");
            }

            long expiresAt = data.path("expires_at").asLong(0);
            if (expiresAt > 0 && expiresAt < System.currentTimeMillis() / 1000) {
                throw new OAuthException("Facebook token has expired");
            }

            log.debug("Facebook token validated successfully for user: {}", data.path("user_id").asText());

        } catch (OAuthException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            throw new OAuthException("Facebook token validation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error validating Facebook token: {}", e.getMessage());
            throw new OAuthException("Failed to validate Facebook token");
        }
    }

    private JsonNode fetchUserInfoJson(String accessToken, String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (RestClientException e) {
            throw new OAuthException("Failed to reach OAuth provider: " + e.getMessage());
        } catch (Exception e) {
            throw new OAuthException("Failed to parse OAuth provider response: " + e.getMessage());
        }
    }

    private OAuthUserInfo fetchGoogleUserInfo(String accessToken) {
        try {
            JsonNode json = fetchUserInfoJson(accessToken, googleUserInfoUrl);
            return OAuthUserInfo.builder()
                    .providerId(json.get("sub").asText())
                    .email(json.get("email").asText())
                    .firstName(json.path("given_name").asText(""))
                    .lastName(json.path("family_name").asText(""))
                    .profileImage(json.path("picture").asText(null))
                    .emailVerified(json.path("email_verified").asBoolean(false))
                    .build();
        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing Google user info response: {}", e.getMessage());
            throw new OAuthException("Failed to process Google authentication");
        }
    }

    private OAuthUserInfo fetchFacebookUserInfo(String accessToken) {
        try {
            JsonNode json = fetchUserInfoJson(accessToken, facebookUserInfoUrl);
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
        } catch (Exception e) {
            log.error("Error parsing Facebook user info response: {}", e.getMessage());
            throw new OAuthException("Failed to process Facebook authentication");
        }
    }

    private AuthResponse processOAuthLogin(OAuthUserInfo userInfo, String provider, Role targetRole) {
        if (userInfo.email() == null || userInfo.email().isBlank()) {
            throw new OAuthException("Email permission is required. Please grant email access and try again.");
        }

        Optional<User> existingByProvider = userRepository.findByOauthProviderAndOauthProviderId(
                provider, userInfo.providerId()
        );

        boolean isNewUser = existingByProvider.isEmpty();
        User user = existingByProvider.orElseGet(() -> createUserFromOAuth(userInfo, provider, targetRole));

        if (user.getStatus() == Status.SUSPENDED) {
            throw new OAuthException("Your account has been suspended. Please contact support.");
        }

        if (isNewUser) {
            emailService.sendWelcomeEmail(user.getEmail(), userInfo.firstName());
        }

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .isVerified(user.getIsVerified())
                .profileCompleted(user.isProfileCompleted())
                .oauthProvider(user.getOauthProvider())
                .oauthProviderId(user.getOauthProviderId())
                .build();

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .user(userResponse)
                .build();
    }

    private User createUserFromOAuth(OAuthUserInfo userInfo, String provider, Role targetRole) {
        userRepository.findByEmail(userInfo.email()).ifPresent(existing -> {
            throw new OAuthException(
                    "An account with this email already exists. Please log in with your password or link "
                            + provider + " from your account settings."
            );
        });

        User user = User.builder()
                .email(userInfo.email())
                .role(targetRole)
                .status(Status.ACTIVE)
                .oauthProvider(provider)
                .oauthProviderId(userInfo.providerId())
                .isVerified(userInfo.emailVerified())
                .profileCompleted(true)
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
        customer.setFirstName(userInfo.firstName());
        customer.setLastName(userInfo.lastName());
        customer.setProfileImage(userInfo.profileImage());
        customer.setWalletBalance(BigDecimal.ZERO);
        customer.setTotalOrders(0);
        customerRepository.save(customer);
    }

    private void createDriverProfile(User user, OAuthUserInfo userInfo) {
        Driver driver = new Driver();
        driver.setUser(user);
        driver.setFirstName(userInfo.firstName());
        driver.setLastName(userInfo.lastName());
        driver.setProfileImage(userInfo.profileImage());
        driver.setIsAvailable(false);
        driver.setRating(BigDecimal.ZERO);
        driver.setTotalDeliveries(0);
        driver.setWalletBalance(BigDecimal.ZERO);
        driver.setVerificationStatus(VerificationStatus.PENDING);
        driver.setVerificationDocuments(new HashMap<>());
        driverRepository.save(driver);
    }

    @Builder
    private record OAuthUserInfo(String providerId, String email, String firstName, String lastName,
                                 String profileImage, boolean emailVerified) {
    }
}