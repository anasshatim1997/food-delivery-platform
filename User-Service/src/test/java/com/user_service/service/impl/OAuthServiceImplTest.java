package com.user_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.response.AuthResponse;
import com.user_service.entity.Customer;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.exception.OAuthException;
import com.user_service.repository.CustomerRepository;
import com.user_service.repository.DriverRepository;
import com.user_service.repository.UserRepository;
import com.user_service.security.JwtService;
import com.user_service.service.IEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private IEmailService emailService;

    @InjectMocks
    private OAuthServiceImpl oAuthService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuthService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(oAuthService, "googleUserInfoUrl", "https://www.googleapis.com/oauth2/v3/userinfo");
        ReflectionTestUtils.setField(oAuthService, "googleTokenInfoUrl", "https://oauth2.googleapis.com/tokeninfo");
        ReflectionTestUtils.setField(oAuthService, "googleClientId", "test-google-client-id");
        ReflectionTestUtils.setField(oAuthService, "facebookUserInfoUrl", "https://graph.facebook.com/me?fields=id,name,email,first_name,last_name");
        ReflectionTestUtils.setField(oAuthService, "facebookAppId", "test-fb-app-id");
        ReflectionTestUtils.setField(oAuthService, "facebookAppSecret", "test-fb-app-secret");
        ReflectionTestUtils.setField(oAuthService, "facebookDebugTokenUrl", "https://graph.facebook.com/debug_token");
    }

    @Nested
    class LoginWithGoogle {

        @Test
        void createsNewCustomerAndReturnsAuthResponseForNewUser() {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":3600,"scope":"email"}
                    """;
            String userInfoJson = """
                    {"sub":"google-123","email":"john@example.com","given_name":"John","family_name":"Doe","picture":"http://img.com/photo.jpg","email_verified":true}
                    """;

            when(restTemplate.getForEntity(contains("tokeninfo"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));
            when(restTemplate.exchange(contains("userinfo"), eq(HttpMethod.GET), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));
            when(userRepository.findByOauthProviderAndOauthProviderId("GOOGLE", "google-123"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

            User savedUser = new User();
            savedUser.setId(UUID.randomUUID());
            savedUser.setEmail("john@example.com");
            savedUser.setRole(Role.CUSTOMER);
            savedUser.setStatus(Status.ACTIVE);
            savedUser.setOauthProvider("GOOGLE");
            savedUser.setOauthProviderId("google-123");
            savedUser.setIsVerified(true);
            savedUser.setProfileCompleted(true);

            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(customerRepository.save(any(Customer.class))).thenReturn(new Customer());
            when(jwtService.generateAccessToken(savedUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(savedUser)).thenReturn("refresh-token");

            AuthResponse result = oAuthService.loginWithGoogle("valid-access-token", Role.CUSTOMER);

            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.getUser().getEmail()).isEqualTo("john@example.com");
            verify(emailService).sendWelcomeEmail("john@example.com", "John");
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        void returnsAuthResponseForExistingGoogleUser() {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":3600}
                    """;
            String userInfoJson = """
                    {"sub":"google-123","email":"john@example.com","given_name":"John","family_name":"Doe","email_verified":true}
                    """;

            User existingUser = new User();
            existingUser.setId(UUID.randomUUID());
            existingUser.setEmail("john@example.com");
            existingUser.setRole(Role.CUSTOMER);
            existingUser.setStatus(Status.ACTIVE);
            existingUser.setIsVerified(true);
            existingUser.setProfileCompleted(true);

            when(restTemplate.getForEntity(contains("tokeninfo"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));
            when(restTemplate.exchange(contains("userinfo"), eq(HttpMethod.GET), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));
            when(userRepository.findByOauthProviderAndOauthProviderId("GOOGLE", "google-123"))
                    .thenReturn(Optional.of(existingUser));
            when(jwtService.generateAccessToken(existingUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(existingUser)).thenReturn("refresh-token");

            AuthResponse result = oAuthService.loginWithGoogle("valid-access-token", Role.CUSTOMER);

            assertThat(result.getAccessToken()).isEqualTo("access-token");
            verify(emailService, never()).sendWelcomeEmail(any(), any());
            verify(userRepository, never()).save(any());
        }

        @Test
        void throwsOAuthExceptionWhenGoogleTokenIsInvalid() {
            String tokenInfoJson = """
                    {"error":"invalid_token","error_description":"Token has been revoked"}
                    """;
            when(restTemplate.getForEntity(contains("tokeninfo"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));

            assertThatThrownBy(() -> oAuthService.loginWithGoogle("bad-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("Invalid Google token");
        }

        @Test
        void throwsOAuthExceptionWhenGoogleTokenAudienceMismatches() {
            String tokenInfoJson = """
                    {"aud":"other-client-id","expires_in":3600}
                    """;
            when(restTemplate.getForEntity(contains("tokeninfo"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));

            assertThatThrownBy(() -> oAuthService.loginWithGoogle("wrong-audience-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("not issued for this application");
        }

        @Test
        void throwsOAuthExceptionWhenGoogleTokenExpired() {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":0}
                    """;
            when(restTemplate.getForEntity(contains("tokeninfo"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));

            assertThatThrownBy(() -> oAuthService.loginWithGoogle("expired-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        void throwsOAuthExceptionWhenUserIsSuspended() {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":3600}
                    """;
            String userInfoJson = """
                    {"sub":"google-123","email":"john@example.com","given_name":"John","family_name":"Doe","email_verified":true}
                    """;

            User suspendedUser = new User();
            suspendedUser.setId(UUID.randomUUID());
            suspendedUser.setEmail("john@example.com");
            suspendedUser.setStatus(Status.SUSPENDED);

            when(restTemplate.getForEntity(contains("tokeninfo"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));
            when(restTemplate.exchange(contains("userinfo"), eq(HttpMethod.GET), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));
            when(userRepository.findByOauthProviderAndOauthProviderId("GOOGLE", "google-123"))
                    .thenReturn(Optional.of(suspendedUser));

            assertThatThrownBy(() -> oAuthService.loginWithGoogle("valid-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("suspended");
        }

        @Test
        void throwsOAuthExceptionWhenEmailAlreadyExistsWithDifferentProvider() {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":3600}
                    """;
            String userInfoJson = """
                    {"sub":"google-new","email":"existing@example.com","given_name":"Jane","family_name":"Doe","email_verified":true}
                    """;

            when(restTemplate.getForEntity(contains("tokeninfo"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));
            when(restTemplate.exchange(contains("userinfo"), eq(HttpMethod.GET), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));
            when(userRepository.findByOauthProviderAndOauthProviderId("GOOGLE", "google-new"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("existing@example.com"))
                    .thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> oAuthService.loginWithGoogle("valid-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        void createsDriverProfileWhenTargetRoleIsDriver() {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":3600}
                    """;
            String userInfoJson = """
                    {"sub":"google-456","email":"driver@example.com","given_name":"Ali","family_name":"Baba","email_verified":true}
                    """;

            when(restTemplate.getForEntity(contains("tokeninfo"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));
            when(restTemplate.exchange(contains("userinfo"), eq(HttpMethod.GET), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));
            when(userRepository.findByOauthProviderAndOauthProviderId("GOOGLE", "google-456"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.empty());

            User savedUser = new User();
            savedUser.setId(UUID.randomUUID());
            savedUser.setEmail("driver@example.com");
            savedUser.setRole(Role.DRIVER);
            savedUser.setStatus(Status.ACTIVE);
            savedUser.setIsVerified(true);
            savedUser.setProfileCompleted(true);

            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(driverRepository.save(any(Driver.class))).thenReturn(new Driver());
            when(jwtService.generateAccessToken(any())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

            oAuthService.loginWithGoogle("valid-access-token", Role.DRIVER);

            verify(driverRepository).save(any(Driver.class));
            verify(customerRepository, never()).save(any(Customer.class));
        }
    }

    @Nested
    class LoginWithFacebook {

        @Test
        void createsNewCustomerAndReturnsAuthResponseForNewFacebookUser() {
            String debugTokenJson = """
                    {"data":{"is_valid":true,"app_id":"test-fb-app-id","expires_at":9999999999,"user_id":"fb-123"}}
                    """;
            String userInfoJson = """
                    {"id":"fb-123","email":"fbuser@example.com","first_name":"Bob","last_name":"Smith"}
                    """;

            when(restTemplate.getForEntity(contains("debug_token"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(debugTokenJson));
            when(restTemplate.exchange(contains("graph.facebook.com/me"), eq(HttpMethod.GET), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));
            when(userRepository.findByOauthProviderAndOauthProviderId("FACEBOOK", "fb-123"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("fbuser@example.com")).thenReturn(Optional.empty());

            User savedUser = new User();
            savedUser.setId(UUID.randomUUID());
            savedUser.setEmail("fbuser@example.com");
            savedUser.setRole(Role.CUSTOMER);
            savedUser.setStatus(Status.ACTIVE);
            savedUser.setIsVerified(false);
            savedUser.setProfileCompleted(true);

            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(customerRepository.save(any(Customer.class))).thenReturn(new Customer());
            when(jwtService.generateAccessToken(savedUser)).thenReturn("fb-access-token");
            when(jwtService.generateRefreshToken(savedUser)).thenReturn("fb-refresh-token");

            AuthResponse result = oAuthService.loginWithFacebook("valid-fb-token", Role.CUSTOMER);

            assertThat(result.getAccessToken()).isEqualTo("fb-access-token");
            verify(emailService).sendWelcomeEmail("fbuser@example.com", "Bob");
        }

        @Test
        void throwsOAuthExceptionWhenFacebookTokenIsInvalid() {
            String debugTokenJson = """
                    {"data":{"is_valid":false,"error":{"message":"Invalid token"}}}
                    """;
            when(restTemplate.getForEntity(contains("debug_token"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(debugTokenJson));

            assertThatThrownBy(() -> oAuthService.loginWithFacebook("bad-fb-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("Invalid Facebook token");
        }

        @Test
        void throwsOAuthExceptionWhenFacebookAppIdMismatches() {
            String debugTokenJson = """
                    {"data":{"is_valid":true,"app_id":"other-app-id","expires_at":9999999999}}
                    """;
            when(restTemplate.getForEntity(contains("debug_token"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(debugTokenJson));

            assertThatThrownBy(() -> oAuthService.loginWithFacebook("wrong-app-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("not issued for this application");
        }

        @Test
        void throwsOAuthExceptionWhenFacebookTokenExpired() {
            String debugTokenJson = """
                    {"data":{"is_valid":true,"app_id":"test-fb-app-id","expires_at":1}}
                    """;
            when(restTemplate.getForEntity(contains("debug_token"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(debugTokenJson));

            assertThatThrownBy(() -> oAuthService.loginWithFacebook("expired-fb-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        void throwsOAuthExceptionWhenFacebookUserInfoHasError() {
            String debugTokenJson = """
                    {"data":{"is_valid":true,"app_id":"test-fb-app-id","expires_at":9999999999}}
                    """;
            String userInfoJson = """
                    {"error":{"message":"Invalid OAuth access token"}}
                    """;

            when(restTemplate.getForEntity(contains("debug_token"), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(debugTokenJson));
            when(restTemplate.exchange(contains("graph.facebook.com/me"), eq(HttpMethod.GET), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));
            when(userRepository.findByOauthProviderAndOauthProviderId("FACEBOOK", any()))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oAuthService.loginWithFacebook("token-with-error-response", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("Invalid Facebook access token");
        }
    }
}