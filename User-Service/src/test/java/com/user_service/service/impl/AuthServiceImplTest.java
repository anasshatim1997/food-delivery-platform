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
import com.user_service.service.IEmailService;
import com.user_service.service.IOAuthService;
import com.user_service.service.IUserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private IOAuthService oAuthService;
    @Mock private IEmailService emailService;
    @Mock private IUserProfileService userProfileService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = User.builder()
                .id(userId)
                .email("john@mail.com")
                .password("encoded_password")
                .phone("0600000000")
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .profileCompleted(true)
                .build();

        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh_token");
    }

    // =========================================================================
    // registerUser
    // =========================================================================

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("saves user, sends verification email, and returns tokens")
        void registerUser_success() {
            // --- Arrange ---
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("new@mail.com");
            request.setPassword("Password1!");
            request.setPhone("0611111111");

            when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);
            when(userRepository.existsByPhone("0611111111")).thenReturn(false);
            when(passwordEncoder.encode("Password1!")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // --- Act ---
            AuthResponse response = authService.registerUser(request);

            // --- Assert ---
            assertThat(response.getAccessToken()).isEqualTo("access_token");
            verify(emailService).sendVerificationEmail(eq("new@mail.com"), anyString());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("throws when email is already taken")
        void registerUser_emailTaken() {
            // --- Arrange ---
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("taken@mail.com");
            request.setPhone("0611111111");
            when(userRepository.existsByEmail("taken@mail.com")).thenReturn(true);

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.registerUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email is already registered");
        }

        @Test
        @DisplayName("throws when phone is already taken")
        void registerUser_phoneTaken() {
            // --- Arrange ---
            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail("new@mail.com");
            request.setPhone("0600000000");
            when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);
            when(userRepository.existsByPhone("0600000000")).thenReturn(true);

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.registerUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Phone number is already registered");
        }
    }

    // =========================================================================
    // registerCustomer
    // =========================================================================

    @Nested
    @DisplayName("registerCustomer")
    class RegisterCustomer {

        @Test
        @DisplayName("creates user, customer profile, sends emails, and returns tokens")
        void registerCustomer_success() {
            // --- Arrange ---
            RegisterRequest request = new RegisterRequest();
            request.setEmail("customer@mail.com");
            request.setPassword("Pass1!");
            request.setPhone("0622222222");
            request.setFirstName("Alice");
            request.setLastName("Smith");

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByPhone(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // --- Act ---
            AuthResponse response = authService.registerCustomer(request);

            // --- Assert ---
            assertThat(response.getAccessToken()).isEqualTo("access_token");
            verify(userProfileService).createCustomerProfile(any(User.class), eq("Alice"), eq("Smith"));
            verify(emailService).sendVerificationEmail(eq("customer@mail.com"), anyString());
            verify(emailService).sendWelcomeEmail(eq("customer@mail.com"), eq("Alice"));
        }
    }

    // =========================================================================
    // registerDriver
    // =========================================================================

    @Nested
    @DisplayName("registerDriver")
    class RegisterDriver {

        @Test
        @DisplayName("creates user, driver profile, sends emails, and returns tokens")
        void registerDriver_success() {
            // --- Arrange ---
            RegisterRequest request = new RegisterRequest();
            request.setEmail("driver@mail.com");
            request.setPassword("Pass1!");
            request.setPhone("0633333333");
            request.setFirstName("Bob");
            request.setLastName("Jones");
            request.setVehicleNumber("ABC-123");
            request.setLicenseNumber("LIC-456");

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByPhone(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // --- Act ---
            AuthResponse response = authService.registerDriver(request);

            // --- Assert ---
            assertThat(response.getAccessToken()).isEqualTo("access_token");
            verify(userProfileService).createDriverProfile(any(User.class), eq("Bob"), eq("Jones"), any(), any(), any());
            verify(emailService).sendWelcomeEmail(eq("driver@mail.com"), eq("Bob"));
        }
    }

    // =========================================================================
    // login
    // =========================================================================

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("authenticates and returns tokens for active user")
        void login_success() {
            // --- Arrange ---
            LoginRequest request = new LoginRequest();
            request.setEmail("john@mail.com");
            request.setPassword("Password1!");

            when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(activeUser));

            // --- Act ---
            AuthResponse response = authService.login(request);

            // --- Assert ---
            assertThat(response.getAccessToken()).isEqualTo("access_token");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("throws when user is suspended")
        void login_suspendedUser() {
            // --- Arrange ---
            activeUser.setStatus(Status.SUSPENDED);
            LoginRequest request = new LoginRequest();
            request.setEmail("john@mail.com");
            request.setPassword("Password1!");
            when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(activeUser));

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("suspended");
        }

        @Test
        @DisplayName("throws when authentication manager rejects credentials")
        void login_badCredentials() {
            // --- Arrange ---
            LoginRequest request = new LoginRequest();
            request.setEmail("john@mail.com");
            request.setPassword("wrong");
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any());

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    // =========================================================================
    // refreshToken
    // =========================================================================

    @Nested
    @DisplayName("refreshToken")
    class RefreshToken {

        @Test
        @DisplayName("issues new tokens for a valid refresh token")
        void refreshToken_success() {
            // --- Arrange ---
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid_refresh");

            when(jwtService.isRefreshToken("valid_refresh")).thenReturn(true);
            when(jwtService.isTokenExpired("valid_refresh")).thenReturn(false);
            when(jwtService.extractUsername("valid_refresh")).thenReturn("john@mail.com");
            when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(activeUser));

            // --- Act ---
            AuthResponse response = authService.refreshToken(request);

            // --- Assert ---
            assertThat(response.getAccessToken()).isEqualTo("access_token");
        }

        @Test
        @DisplayName("throws when token is not a refresh token")
        void refreshToken_notRefreshToken() {
            // --- Arrange ---
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("access_token_not_refresh");
            when(jwtService.isRefreshToken("access_token_not_refresh")).thenReturn(false);

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a refresh token");
        }

        @Test
        @DisplayName("throws when refresh token is expired")
        void refreshToken_expired() {
            // --- Arrange ---
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("expired_refresh");
            when(jwtService.isRefreshToken("expired_refresh")).thenReturn(true);
            when(jwtService.isTokenExpired("expired_refresh")).thenReturn(true);

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired");
        }
    }

    // =========================================================================
    // verifyEmail
    // =========================================================================

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("marks user as verified and clears the code")
        void verifyEmail_success() {
            // --- Arrange ---
            activeUser.setIsVerified(false);
            activeUser.setVerificationCode("valid_code");
            activeUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
            when(userRepository.findByVerificationCode("valid_code")).thenReturn(Optional.of(activeUser));

            // --- Act ---
            authService.verifyEmail("valid_code");

            // --- Assert ---
            assertThat(activeUser.getIsVerified()).isTrue();
            assertThat(activeUser.getVerificationCode()).isNull();
            verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("throws when verification code is expired")
        void verifyEmail_expiredCode() {
            // --- Arrange ---
            activeUser.setVerificationCode("old_code");
            activeUser.setVerificationCodeExpiresAt(LocalDateTime.now().minusHours(1));
            when(userRepository.findByVerificationCode("old_code")).thenReturn(Optional.of(activeUser));

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.verifyEmail("old_code"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("throws when verification code does not exist")
        void verifyEmail_invalidCode() {
            // --- Arrange ---
            when(userRepository.findByVerificationCode("bad_code")).thenReturn(Optional.empty());

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.verifyEmail("bad_code"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid or expired");
        }
    }

    // =========================================================================
    // resendVerificationEmail
    // =========================================================================

    @Nested
    @DisplayName("resendVerificationEmail")
    class ResendVerificationEmail {

        @Test
        @DisplayName("generates new code and resends email for unverified user")
        void resendVerification_success() {
            // --- Arrange ---
            activeUser.setIsVerified(false);
            when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(activeUser));

            // --- Act ---
            authService.resendVerificationEmail("john@mail.com");

            // --- Assert ---
            verify(userRepository).save(activeUser);
            verify(emailService).sendVerificationEmail(eq("john@mail.com"), anyString());
        }

        @Test
        @DisplayName("throws when user is already verified")
        void resendVerification_alreadyVerified() {
            // --- Arrange ---
            activeUser.setIsVerified(true);
            when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(activeUser));

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.resendVerificationEmail("john@mail.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already verified");
        }
    }

    // =========================================================================
    // forgotPassword
    // =========================================================================

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("sets reset token and sends email")
        void forgotPassword_success() {
            // --- Arrange ---
            when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(activeUser));

            // --- Act ---
            authService.forgotPassword("john@mail.com");

            // --- Assert ---
            assertThat(activeUser.getPasswordResetToken()).isNotNull();
            verify(emailService).sendPasswordResetEmail(eq("john@mail.com"), anyString());
        }

        @Test
        @DisplayName("throws for OAuth-only user with no password")
        void forgotPassword_oauthUser() {
            // --- Arrange ---
            activeUser.setOauthProvider("GOOGLE");
            activeUser.setPassword(null);
            when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(activeUser));

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.forgotPassword("john@mail.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("GOOGLE");
        }

        @Test
        @DisplayName("throws when email not found")
        void forgotPassword_emailNotFound() {
            // --- Arrange ---
            when(userRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.forgotPassword("nobody@mail.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // resetPassword
    // =========================================================================

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("resets password, clears token, and sends notification")
        void resetPassword_success() {
            // --- Arrange ---
            activeUser.setPasswordResetToken("reset_token");
            activeUser.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
            when(userRepository.findByPasswordResetToken("reset_token")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.encode("NewPass1!")).thenReturn("new_encoded");

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("reset_token");
            request.setNewPassword("NewPass1!");

            // --- Act ---
            authService.resetPassword(request);

            // --- Assert ---
            assertThat(activeUser.getPassword()).isEqualTo("new_encoded");
            assertThat(activeUser.getPasswordResetToken()).isNull();
            verify(emailService).sendPasswordChangedNotification(activeUser.getEmail());
        }

        @Test
        @DisplayName("throws when reset token is expired")
        void resetPassword_expiredToken() {
            // --- Arrange ---
            activeUser.setPasswordResetToken("expired_token");
            activeUser.setPasswordResetTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByPasswordResetToken("expired_token")).thenReturn(Optional.of(activeUser));

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("expired_token");
            request.setNewPassword("NewPass1!");

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired");
        }
    }

    // =========================================================================
    // changePassword
    // =========================================================================

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("changes password and sends notification")
        void changePassword_success() {
            // --- Arrange ---
            when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("OldPass1!", "encoded_password")).thenReturn(true);
            when(passwordEncoder.encode("NewPass1!")).thenReturn("new_encoded");

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("OldPass1!");
            request.setNewPassword("NewPass1!");

            // --- Act ---
            authService.changePassword(userId, request);

            // --- Assert ---
            assertThat(activeUser.getPassword()).isEqualTo("new_encoded");
            verify(emailService).sendPasswordChangedNotification(activeUser.getEmail());
        }

        @Test
        @DisplayName("throws when current password is wrong")
        void changePassword_wrongCurrentPassword() {
            // --- Arrange ---
            when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("WrongPass!", "encoded_password")).thenReturn(false);

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("WrongPass!");
            request.setNewPassword("NewPass1!");

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.changePassword(userId, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("incorrect");
        }

        @Test
        @DisplayName("throws when user has no password set (OAuth-only account)")
        void changePassword_noPasswordSet() {
            // --- Arrange ---
            activeUser.setPassword(null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("any");
            request.setNewPassword("NewPass1!");

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.changePassword(userId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No password");
        }
    }

    // =========================================================================
    // oauthLogin
    // =========================================================================

    @Nested
    @DisplayName("oauthLogin")
    class OAuthLogin {

        @Test
        @DisplayName("delegates to oAuthService for GOOGLE provider")
        void oauthLogin_google() {
            // --- Arrange ---
            OAuthLoginRequest request = new OAuthLoginRequest();
            request.setProvider("GOOGLE");
            request.setAccessToken("google_token");
            request.setTargetRole(Role.CUSTOMER);

            AuthResponse expected = AuthResponse.builder().accessToken("google_access").build();
            when(oAuthService.loginWithGoogle("google_token", Role.CUSTOMER)).thenReturn(expected);

            // --- Act ---
            AuthResponse response = authService.oauthLogin(request);

            // --- Assert ---
            assertThat(response.getAccessToken()).isEqualTo("google_access");
        }

        @Test
        @DisplayName("throws for unsupported OAuth provider")
        void oauthLogin_unsupportedProvider() {
            // --- Arrange ---
            OAuthLoginRequest request = new OAuthLoginRequest();
            request.setProvider("TWITTER");
            request.setAccessToken("token");

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.oauthLogin(request))
                    .isInstanceOf(OAuthException.class)
                    .hasMessageContaining("Unsupported");
        }
    }

    // =========================================================================
    // setPasswordForOAuthUser
    // =========================================================================

    @Nested
    @DisplayName("setPasswordForOAuthUser")
    class SetPasswordForOAuthUser {

        @Test
        @DisplayName("sets password for OAuth user who has none")
        void setPassword_success() {
            // --- Arrange ---
            activeUser.setPassword(null);
            activeUser.setOauthProvider("GOOGLE");
            when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.encode("NewPass1!")).thenReturn("encoded_new");

            SetPasswordRequest request = new SetPasswordRequest();
            request.setPassword("NewPass1!");

            // --- Act ---
            AuthResponse response = authService.setPasswordForOAuthUser(userId, request);

            // --- Assert ---
            assertThat(activeUser.getPassword()).isEqualTo("encoded_new");
            assertThat(response.getAccessToken()).isEqualTo("access_token");
            verify(emailService).sendPasswordChangedNotification(activeUser.getEmail());
        }

        @Test
        @DisplayName("throws when user already has a password")
        void setPassword_alreadyHasPassword() {
            // --- Arrange ---
            when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

            SetPasswordRequest request = new SetPasswordRequest();
            request.setPassword("NewPass1!");

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.setPasswordForOAuthUser(userId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already set");
        }

        @Test
        @DisplayName("throws when user is not an OAuth user")
        void setPassword_notOAuthUser() {
            // --- Arrange ---
            activeUser.setPassword(null);
            activeUser.setOauthProvider(null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

            SetPasswordRequest request = new SetPasswordRequest();
            request.setPassword("NewPass1!");

            // --- Act & Assert ---
            assertThatThrownBy(() -> authService.setPasswordForOAuthUser(userId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OAuth");
        }
    }
}