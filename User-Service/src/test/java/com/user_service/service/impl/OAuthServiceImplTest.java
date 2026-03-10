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
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private JwtService jwtService;
    @Mock private RestTemplate restTemplate;
    @Mock private IEmailService emailService;

    @InjectMocks
    private OAuthServiceImpl oAuthService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuthService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(oAuthService, "googleUserInfoUrl", "https://www.googleapis.com/oauth2/v3/userinfo");
        ReflectionTestUtils.setField(oAuthService, "googleTokenInfoUrl", "https://oauth2.googleapis.com/tokeninfo");
        ReflectionTestUtils.setField(oAuthService, "googleClientId", "test-google-client-id");
        ReflectionTestUtils.setField(oAuthService, "facebookUserInfoUrl", "https://graph.facebook.com/me");
        ReflectionTestUtils.setField(oAuthService, "facebookAppId", "test-fb-app-id");
        ReflectionTestUtils.setField(oAuthService, "facebookAppSecret", "test-fb-app-secret");
        ReflectionTestUtils.setField(oAuthService, "facebookDebugTokenUrl", "https://graph.facebook.com/debug_token");
    }

    private User buildUser(Role role, Status status) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole(role);
        user.setStatus(status);
        user.setIsVerified(true);
        user.setProfileCompleted(true);
        user.setOauthProvider("GOOGLE");
        user.setOauthProviderId("google-provider-id");
        return user;
    }

    @Nested
    class LoginWithGoogle {

        @Test
        void returnsAuthResponse_forExistingUser() throws Exception {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":3600,"scope":"email"}
                    """;
            String userInfoJson = """
                    {"sub":"google-provider-id","email":"user@example.com","given_name":"John","family_name":"Doe","email_verified":true}
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));

            User user = buildUser(Role.CUSTOMER, Status.ACTIVE);
            when(userRepository.findByOauthProviderAndOauthProviderId("GOOGLE", "google-provider-id"))
                    .thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(user)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

            AuthResponse response = oAuthService.loginWithGoogle("valid-token", Role.CUSTOMER);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
            verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
        }

        @Test
        void createsNewUserAndSendsWelcomeEmail_whenUserDoesNotExist() throws Exception {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":3600,"scope":"email"}
                    """;
            String userInfoJson = """
                    {"sub":"new-google-id","email":"newuser@example.com","given_name":"Jane","family_name":"Doe","email_verified":true}
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));

            when(userRepository.findByOauthProviderAndOauthProviderId("GOOGLE", "new-google-id"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());

            User savedUser = buildUser(Role.CUSTOMER, Status.ACTIVE);
            savedUser.setEmail("newuser@example.com");
            savedUser.setOauthProviderId("new-google-id");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

            AuthResponse response = oAuthService.loginWithGoogle("valid-token", Role.CUSTOMER);

            assertThat(response).isNotNull();
            verify(emailService).sendWelcomeEmail(eq("newuser@example.com"), eq("Jane"));
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        void createsDriverProfile_whenTargetRoleIsDriver() throws Exception {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":3600,"scope":"email"}
                    """;
            String userInfoJson = """
                    {"sub":"driver-google-id","email":"driver@example.com","given_name":"Driver","family_name":"One","email_verified":true}
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));

            when(userRepository.findByOauthProviderAndOauthProviderId("GOOGLE", "driver-google-id"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.empty());

            User savedUser = buildUser(Role.DRIVER, Status.ACTIVE);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateAccessToken(any())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

            oAuthService.loginWithGoogle("valid-token", Role.DRIVER);

            verify(driverRepository).save(any(Driver.class));
            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        void throwsOAuthException_whenTokenHasError() throws Exception {
            String tokenInfoJson = """
                    {"error":"invalid_token","error_description":"Token has been expired or revoked"}
                    """;
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));

            assertThatThrownBy(() -> oAuthService.loginWithGoogle("expired-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("Invalid Google token");
        }

        @Test
        void throwsOAuthException_whenAudienceMismatch() throws Exception {
            String tokenInfoJson = """
                    {"aud":"different-client-id","expires_in":3600}
                    """;
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));

            assertThatThrownBy(() -> oAuthService.loginWithGoogle("valid-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("not issued for this application");
        }

        @Test
        void throwsOAuthException_whenTokenIsExpired() throws Exception {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":0}
                    """;
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));

            assertThatThrownBy(() -> oAuthService.loginWithGoogle("expired-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        void throwsOAuthException_whenUserIsSuspended() throws Exception {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":3600}
                    """;
            String userInfoJson = """
                    {"sub":"google-provider-id","email":"user@example.com","given_name":"John","family_name":"Doe","email_verified":true}
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));

            User suspendedUser = buildUser(Role.CUSTOMER, Status.SUSPENDED);
            when(userRepository.findByOauthProviderAndOauthProviderId("GOOGLE", "google-provider-id"))
                    .thenReturn(Optional.of(suspendedUser));

            assertThatThrownBy(() -> oAuthService.loginWithGoogle("valid-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("suspended");
        }

        @Test
        void throwsOAuthException_whenRestClientFails() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenThrow(HttpClientErrorException.BadRequest.class);

            assertThatThrownBy(() -> oAuthService.loginWithGoogle("bad-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("Google token validation failed");
        }

        @Test
        void throwsOAuthException_whenEmailAlreadyExistsWithDifferentProvider() throws Exception {
            String tokenInfoJson = """
                    {"aud":"test-google-client-id","expires_in":3600}
                    """;
            String userInfoJson = """
                    {"sub":"new-sub","email":"existing@example.com","given_name":"John","family_name":"Doe","email_verified":true}
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(tokenInfoJson));
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));

            when(userRepository.findByOauthProviderAndOauthProviderId("GOOGLE", "new-sub"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("existing@example.com"))
                    .thenReturn(Optional.of(buildUser(Role.CUSTOMER, Status.ACTIVE)));

            assertThatThrownBy(() -> oAuthService.loginWithGoogle("valid-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("account with this email already exists");
        }
    }

    @Nested
    class LoginWithFacebook {

        @Test
        void returnsAuthResponse_forExistingUser() throws Exception {
            String debugTokenJson = """
                    {"data":{"is_valid":true,"app_id":"test-fb-app-id","expires_at":0}}
                    """;
            String userInfoJson = """
                    {"id":"fb-provider-id","email":"fbuser@example.com","first_name":"Mark","last_name":"Z"}
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(debugTokenJson));
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));

            User user = buildUser(Role.CUSTOMER, Status.ACTIVE);
            user.setOauthProvider("FACEBOOK");
            user.setOauthProviderId("fb-provider-id");
            when(userRepository.findByOauthProviderAndOauthProviderId("FACEBOOK", "fb-provider-id"))
                    .thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(user)).thenReturn("fb-access-token");
            when(jwtService.generateRefreshToken(user)).thenReturn("fb-refresh-token");

            AuthResponse response = oAuthService.loginWithFacebook("fb-valid-token", Role.CUSTOMER);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("fb-access-token");
            verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
        }

        @Test
        void throwsOAuthException_whenFacebookTokenIsInvalid() throws Exception {
            String debugTokenJson = """
                    {"data":{"is_valid":false,"error":{"message":"Invalid OAuth access token"}}}
                    """;
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(debugTokenJson));

            assertThatThrownBy(() -> oAuthService.loginWithFacebook("invalid-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("Invalid Facebook token");
        }

        @Test
        void throwsOAuthException_whenFacebookAppIdMismatch() throws Exception {
            String debugTokenJson = """
                    {"data":{"is_valid":true,"app_id":"wrong-app-id","expires_at":0}}
                    """;
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(debugTokenJson));

            assertThatThrownBy(() -> oAuthService.loginWithFacebook("valid-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("not issued for this application");
        }

        @Test
        void throwsOAuthException_whenFacebookTokenExpired() throws Exception {
            long pastTimestamp = (System.currentTimeMillis() / 1000) - 3600;
            String debugTokenJson = String.format("""
                    {"data":{"is_valid":true,"app_id":"test-fb-app-id","expires_at":%d}}
                    """, pastTimestamp);

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(debugTokenJson));

            assertThatThrownBy(() -> oAuthService.loginWithFacebook("expired-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        void throwsOAuthException_whenFacebookUserInfoHasError() throws Exception {
            String debugTokenJson = """
                    {"data":{"is_valid":true,"app_id":"test-fb-app-id","expires_at":0}}
                    """;
            String userInfoJson = """
                    {"error":{"message":"Invalid OAuth access token","type":"OAuthException","code":190}}
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(debugTokenJson));
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));

            assertThatThrownBy(() -> oAuthService.loginWithFacebook("bad-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("Invalid Facebook access token");
        }

        @Test
        void throwsOAuthException_whenFacebookRestClientFails() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenThrow(HttpClientErrorException.Unauthorized.class);

            assertThatThrownBy(() -> oAuthService.loginWithFacebook("bad-token", Role.CUSTOMER))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("Facebook token validation failed");
        }

        @Test
        void createsNewUserAndSendsWelcomeEmail_whenFacebookUserDoesNotExist() throws Exception {
            String debugTokenJson = """
                    {"data":{"is_valid":true,"app_id":"test-fb-app-id","expires_at":0}}
                    """;
            String userInfoJson = """
                    {"id":"new-fb-id","email":"newfb@example.com","first_name":"New","last_name":"User"}
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(debugTokenJson));
            when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(userInfoJson));

            when(userRepository.findByOauthProviderAndOauthProviderId("FACEBOOK", "new-fb-id"))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail("newfb@example.com")).thenReturn(Optional.empty());

            User savedUser = buildUser(Role.CUSTOMER, Status.ACTIVE);
            savedUser.setEmail("newfb@example.com");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateAccessToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

            oAuthService.loginWithFacebook("fb-token", Role.CUSTOMER);

            verify(emailService).sendWelcomeEmail(eq("newfb@example.com"), eq("New"));
            verify(customerRepository).save(any(Customer.class));
        }
    }
}