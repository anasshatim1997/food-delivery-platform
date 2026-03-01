package com.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.request.*;
import com.user_service.dto.response.AuthResponse;
import com.user_service.dto.response.UserResponse;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.enums.VehicleType;
import com.user_service.service.IAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IAuthService authService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    private UUID userId;
    private UserResponse userResponse;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();

        userId = UUID.randomUUID();

        // Setup mock authentication
        when(authentication.getName()).thenReturn(userId.toString());

        // Setup test data
        userResponse = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .phone("+212612345678")
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .isVerified(false)
                .profileCompleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        authResponse = AuthResponse.builder()
                .accessToken("access_token_123")
                .refreshToken("refresh_token_456")
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    // ==================== REGISTER USER TESTS ====================

    @Test
    @DisplayName("Should register user successfully")
    void testRegisterUser_Success() throws Exception {
        // Given
        RegisterUserRequest request = RegisterUserRequest.builder()
                .email("test@example.com")
                .password("StrongP@ss123")
                .phone("+212612345678")
                .build();

        when(authService.registerUser(any(RegisterUserRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("User registered successfully. Please complete your profile."))
                .andExpect(jsonPath("$.data.accessToken").value("access_token_123"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh_token_456"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));

        verify(authService, times(1)).registerUser(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("Should fail to register user with invalid email")
    void testRegisterUser_InvalidEmail() throws Exception {
        // Given
        RegisterUserRequest request = RegisterUserRequest.builder()
                .email("invalid-email")
                .password("StrongP@ss123")
                .phone("+212612345678")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerUser(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("Should fail to register user with missing required fields")
    void testRegisterUser_MissingRequiredFields() throws Exception {
        // Given
        RegisterUserRequest request = RegisterUserRequest.builder()
                .email("test@example.com")
                // Missing password
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerUser(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("Should fail to register user with invalid phone format")
    void testRegisterUser_InvalidPhoneFormat() throws Exception {
        // Given
        RegisterUserRequest request = RegisterUserRequest.builder()
                .email("test@example.com")
                .password("StrongP@ss123")
                .phone("123456789") // Invalid format
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerUser(any(RegisterUserRequest.class));
    }

    // ==================== COMPLETE CUSTOMER PROFILE TESTS ====================

    @Test
    @DisplayName("Should complete customer profile successfully")
    void testCompleteCustomerProfile_Success() throws Exception {
        // Given
        CompleteCustomerProfileRequest request = CompleteCustomerProfileRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .build();

        when(authService.completeCustomerProfile(eq(userId), any(CompleteCustomerProfileRequest.class)))
                .thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/complete-profile/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Customer profile completed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("access_token_123"));

        verify(authService, times(1)).completeCustomerProfile(eq(userId), any(CompleteCustomerProfileRequest.class));
    }

    @Test
    @DisplayName("Should fail to complete customer profile with missing firstName")
    void testCompleteCustomerProfile_MissingFirstName() throws Exception {
        // Given
        CompleteCustomerProfileRequest request = CompleteCustomerProfileRequest.builder()
                .lastName("Doe")
                // Missing firstName
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/v1/complete-profile/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).completeCustomerProfile(any(UUID.class), any(CompleteCustomerProfileRequest.class));
    }

    // ==================== COMPLETE DRIVER PROFILE TESTS ====================

    @Test
    @DisplayName("Should complete driver profile successfully")
    void testCompleteDriverProfile_Success() throws Exception {
        // Given
        CompleteDriverProfileRequest request = CompleteDriverProfileRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .vehicleType(VehicleType.CAR)
                .vehicleNumber("ABC123")
                .licenseNumber("LIC456")
                .build();

        when(authService.completeDriverProfile(eq(userId), any(CompleteDriverProfileRequest.class)))
                .thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/complete-profile/driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Driver profile completed successfully"));

        verify(authService, times(1)).completeDriverProfile(eq(userId), any(CompleteDriverProfileRequest.class));
    }

    @Test
    @DisplayName("Should fail to complete driver profile with missing vehicle type")
    void testCompleteDriverProfile_MissingVehicleType() throws Exception {
        // Given
        CompleteDriverProfileRequest request = CompleteDriverProfileRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                // Missing vehicleType
                .vehicleNumber("ABC123")
                .licenseNumber("LIC456")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/v1/complete-profile/driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).completeDriverProfile(any(UUID.class), any(CompleteDriverProfileRequest.class));
    }

    // ==================== REGISTER CUSTOMER TESTS ====================

    @Test
    @DisplayName("Should register customer successfully")
    void testRegisterCustomer_Success() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("customer@example.com")
                .password("StrongP@ss123")
                .phone("+212612345678")
                .firstName("John")
                .lastName("Customer")
                .build();

        when(authService.registerCustomer(any(RegisterRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Customer registered successfully"));

        verify(authService, times(1)).registerCustomer(any(RegisterRequest.class));
    }

    // ==================== REGISTER DRIVER TESTS ====================

    @Test
    @DisplayName("Should register driver successfully")
    void testRegisterDriver_Success() throws Exception {
        // Given
        Map<String, String> documents = new HashMap<>();
        documents.put("license", "https://example.com/license.jpg");

        RegisterRequest request = RegisterRequest.builder()
                .email("driver@example.com")
                .password("StrongP@ss123")
                .phone("+212612345678")
                .firstName("Jane")
                .lastName("Driver")
                .vehicleType(VehicleType.CAR)
                .vehicleNumber("ABC123")
                .licenseNumber("LIC456")
                .verificationDocuments(documents)
                .build();

        when(authService.registerDriver(any(RegisterRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/register/driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Driver registered successfully"));

        verify(authService, times(1)).registerDriver(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should fail to register driver with invalid document URL")
    void testRegisterDriver_InvalidDocumentURL() throws Exception {
        // Given
        Map<String, String> documents = new HashMap<>();
        documents.put("license", "not-a-valid-url");

        RegisterRequest request = RegisterRequest.builder()
                .email("driver@example.com")
                .password("StrongP@ss123")
                .phone("+212612345678")
                .firstName("Jane")
                .lastName("Driver")
                .vehicleType(VehicleType.CAR)
                .vehicleNumber("ABC123")
                .licenseNumber("LIC456")
                .verificationDocuments(documents)
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/v1/register/driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerDriver(any(RegisterRequest.class));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login successfully")
    void testLogin_Success() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("StrongP@ss123")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access_token_123"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should fail to login with invalid email format")
    void testLogin_InvalidEmail() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("invalid-email")
                .password("StrongP@ss123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should fail to login with missing password")
    void testLogin_MissingPassword() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                // Missing password
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Test
    @DisplayName("Should refresh token successfully")
    void testRefreshToken_Success() throws Exception {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("refresh_token_456")
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Token refreshed"));

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("Should fail to refresh token with missing refresh token")
    void testRefreshToken_MissingToken() throws Exception {
        // Given
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                // Missing refreshToken
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/v1/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).refreshToken(any(RefreshTokenRequest.class));
    }

    // ==================== OAUTH LOGIN TESTS ====================

    @Test
    @DisplayName("Should OAuth login successfully")
    void testOAuthLogin_Success() throws Exception {
        // Given
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .provider("google")
                .accessToken("google_access_token")
                .targetRole(Role.CUSTOMER)
                .build();

        when(authService.oauthLogin(any(OAuthLoginRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("OAuth login successful"));

        verify(authService, times(1)).oauthLogin(any(OAuthLoginRequest.class));
    }

    @Test
    @DisplayName("Should fail OAuth login with missing provider")
    void testOAuthLogin_MissingProvider() throws Exception {
        // Given
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                // Missing provider
                .accessToken("google_access_token")
                .targetRole(Role.CUSTOMER)
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/v1/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).oauthLogin(any(OAuthLoginRequest.class));
    }

    @Test
    @DisplayName("Should fail OAuth login with missing target role")
    void testOAuthLogin_MissingTargetRole() throws Exception {
        // Given
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .provider("google")
                .accessToken("google_access_token")
                // Missing targetRole
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/v1/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).oauthLogin(any(OAuthLoginRequest.class));
    }

    // ==================== LINK OAUTH PROVIDER TESTS ====================

    @Test
    @DisplayName("Should link OAuth provider successfully")
    void testLinkOAuthProvider_Success() throws Exception {
        // Given
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .provider("google")
                .accessToken("google_access_token")
                .targetRole(Role.CUSTOMER)
                .build();

        when(authService.linkOAuthProvider(eq(userId), any(OAuthLoginRequest.class)))
                .thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/oauth/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("OAuth provider linked successfully"));

        verify(authService, times(1)).linkOAuthProvider(eq(userId), any(OAuthLoginRequest.class));
    }

    // ==================== SET PASSWORD TESTS ====================

    @Test
    @DisplayName("Should set password for OAuth user successfully")
    void testSetPassword_Success() throws Exception {
        // Given
        SetPasswordRequest request = new SetPasswordRequest();
        request.setPassword("NewStrongP@ss123");

        when(authService.setPasswordForOAuthUser(eq(userId), any(SetPasswordRequest.class)))
                .thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Password set successfully"));

        verify(authService, times(1)).setPasswordForOAuthUser(eq(userId), any(SetPasswordRequest.class));
    }

    @Test
    @DisplayName("Should fail to set password with missing password")
    void testSetPassword_MissingPassword() throws Exception {
        // Given
        SetPasswordRequest request = new SetPasswordRequest();
        // Missing password

        // When & Then
        mockMvc.perform(post("/api/auth/v1/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).setPasswordForOAuthUser(any(UUID.class), any(SetPasswordRequest.class));
    }

    // ==================== VERIFY EMAIL TESTS ====================

    @Test
    @DisplayName("Should verify email successfully")
    void testVerifyEmail_Success() throws Exception {
        // Given
        String token = "verification_token_123";
        doNothing().when(authService).verifyEmail(token);

        // When & Then
        mockMvc.perform(get("/api/auth/v1/verify-email")
                        .param("token", token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        verify(authService, times(1)).verifyEmail(token);
    }

    // ==================== RESEND VERIFICATION TESTS ====================

    @Test
    @DisplayName("Should resend verification email successfully")
    void testResendVerification_Success() throws Exception {
        // Given
        String email = "test@example.com";
        doNothing().when(authService).resendVerificationEmail(email);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/resend-verification")
                        .param("email", email))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Verification email sent"));

        verify(authService, times(1)).resendVerificationEmail(email);
    }

    // ==================== FORGOT PASSWORD TESTS ====================

    @Test
    @DisplayName("Should handle forgot password request successfully")
    void testForgotPassword_Success() throws Exception {
        // Given
        String email = "test@example.com";
        doNothing().when(authService).forgotPassword(email);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/forgot-password")
                        .param("email", email))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("If an account with that email exists, a password reset link has been sent."));

        verify(authService, times(1)).forgotPassword(email);
    }

    // ==================== RESET PASSWORD TESTS ====================

    @Test
    @DisplayName("Should reset password successfully")
    void testResetPassword_Success() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset_token_123");
        request.setNewPassword("NewStrongP@ss123");

        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/api/auth/v1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        verify(authService, times(1)).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    @DisplayName("Should fail to reset password with missing token")
    void testResetPassword_MissingToken() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        // Missing token
        request.setNewPassword("NewStrongP@ss123");

        // When & Then
        mockMvc.perform(post("/api/auth/v1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    @DisplayName("Should fail to reset password with missing new password")
    void testResetPassword_MissingNewPassword() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset_token_123");
        // Missing newPassword

        // When & Then
        mockMvc.perform(post("/api/auth/v1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).resetPassword(any(ResetPasswordRequest.class));
    }

    // ==================== CHANGE PASSWORD TESTS ====================

    @Test
    @DisplayName("Should change password successfully")
    void testChangePassword_Success() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldP@ss123");
        request.setNewPassword("NewStrongP@ss123");

        doNothing().when(authService).changePassword(eq(userId), any(ChangePasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/api/auth/v1/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(authService, times(1)).changePassword(eq(userId), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("Should fail to change password with missing current password")
    void testChangePassword_MissingCurrentPassword() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        // Missing currentPassword
        request.setNewPassword("NewStrongP@ss123");

        // When & Then
        mockMvc.perform(post("/api/auth/v1/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).changePassword(any(UUID.class), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("Should fail to change password with missing new password")
    void testChangePassword_MissingNewPassword() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldP@ss123");
        // Missing newPassword

        // When & Then
        mockMvc.perform(post("/api/auth/v1/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(authService, never()).changePassword(any(UUID.class), any(ChangePasswordRequest.class));
    }

    // ==================== AUTHENTICATION EXTRACTION TESTS ====================

    @Test
    @DisplayName("Should extract user ID from authentication correctly")
    void testAuthenticationExtraction() throws Exception {
        // Given
        UUID testUserId = UUID.randomUUID();
        when(authentication.getName()).thenReturn(testUserId.toString());

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldP@ss123");
        request.setNewPassword("NewStrongP@ss123");

        doNothing().when(authService).changePassword(testUserId, request);

        // When & Then
        mockMvc.perform(post("/api/auth/v1/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk());

        verify(authService, times(1)).changePassword(eq(testUserId), any(ChangePasswordRequest.class));
    }
}