package com.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.request.UpdateCustomerRequest;
import com.user_service.dto.request.UpdateDriverRequest;
import com.user_service.dto.response.CustomerProfileResponse;
import com.user_service.dto.response.DriverProfileResponse;
import com.user_service.dto.response.UserProfileResponse;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.enums.VehicleType;
import com.user_service.service.IUserService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NOTE: This test requires CustomerProfileResponse and DriverProfileResponse classes.
 * If they don't exist in your project, create them as subclasses of UserProfileResponse:
 *
 * CustomerProfileResponse should have: firstName, lastName, profileImage
 * DriverProfileResponse should have: firstName, lastName, profileImage, vehicleType, vehicleNumber, licenseNumber, isAvailable
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IUserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;

    private UUID userId;
    private CustomerProfileResponse customerProfileResponse;
    private DriverProfileResponse driverProfileResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();

        userId = UUID.randomUUID();

        // Setup mock authentication
        when(authentication.getName()).thenReturn(userId.toString());

        // Setup customer profile response
        customerProfileResponse = CustomerProfileResponse.builder()
                .id(userId)
                .email("customer@example.com")
                .phone("+212612345678")
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .firstName("John")
                .lastName("Doe")
                .profileImage("https://example.com/profile.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup driver profile response
        driverProfileResponse = DriverProfileResponse.builder()
                .id(userId)
                .email("driver@example.com")
                .phone("+212612345678")
                .role(Role.DRIVER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .firstName("Jane")
                .lastName("Smith")
                .profileImage("https://example.com/profile.jpg")
                .vehicleType(VehicleType.CAR)
                .vehicleNumber("ABC123")
                .licenseNumber("LIC456")
                .isAvailable(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== GET PROFILE TESTS ====================

    @Test
    @DisplayName("Should get customer profile successfully")
    void testGetProfile_CustomerSuccess() throws Exception {
        // Given
        when(userService.getProfile(userId)).thenReturn(customerProfileResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Profile retrieved"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("customer@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));

        verify(userService, times(1)).getProfile(userId);
    }

    @Test
    @DisplayName("Should get driver profile successfully")
    void testGetProfile_DriverSuccess() throws Exception {
        // Given
        when(userService.getProfile(userId)).thenReturn(driverProfileResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Profile retrieved"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("driver@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("Jane"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.role").value("DRIVER"))
                .andExpect(jsonPath("$.data.vehicleType").value("CAR"))
                .andExpect(jsonPath("$.data.vehicleNumber").value("ABC123"))
                .andExpect(jsonPath("$.data.licenseNumber").value("LIC456"))
                .andExpect(jsonPath("$.data.isAvailable").value(true));

        verify(userService, times(1)).getProfile(userId);
    }

    @Test
    @DisplayName("Should extract user ID from authentication correctly for get profile")
    void testGetProfile_AuthenticationExtraction() throws Exception {
        // Given
        UUID testUserId = UUID.randomUUID();
        when(authentication.getName()).thenReturn(testUserId.toString());
        when(userService.getProfile(testUserId)).thenReturn(customerProfileResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userService, times(1)).getProfile(testUserId);
    }

    // ==================== UPDATE CUSTOMER PROFILE TESTS ====================

    @Test
    @DisplayName("Should update customer profile successfully")
    void testUpdateCustomerProfile_Success() throws Exception {
        // Given
        UpdateCustomerRequest request = UpdateCustomerRequest.builder()
                .firstName("John Updated")
                .lastName("Doe Updated")
                .profileImage("https://example.com/new-profile.jpg")
                .build();

        CustomerProfileResponse updatedProfile = CustomerProfileResponse.builder()
                .id(userId)
                .email("customer@example.com")
                .phone("+212612345678")
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .firstName("John Updated")
                .lastName("Doe Updated")
                .profileImage("https://example.com/new-profile.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.updateCustomerProfile(eq(userId), any(UpdateCustomerRequest.class)))
                .thenReturn(updatedProfile);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/me/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Customer profile updated"))
                .andExpect(jsonPath("$.data.firstName").value("John Updated"))
                .andExpect(jsonPath("$.data.lastName").value("Doe Updated"))
                .andExpect(jsonPath("$.data.profileImage").value("https://example.com/new-profile.jpg"));

        verify(userService, times(1)).updateCustomerProfile(eq(userId), any(UpdateCustomerRequest.class));
    }

    @Test
    @DisplayName("Should update customer profile with partial data")
    void testUpdateCustomerProfile_PartialUpdate() throws Exception {
        // Given - Only updating firstName
        UpdateCustomerRequest request = UpdateCustomerRequest.builder()
                .firstName("John Updated")
                .build();

        CustomerProfileResponse updatedProfile = CustomerProfileResponse.builder()
                .id(userId)
                .email("customer@example.com")
                .phone("+212612345678")
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .firstName("John Updated")
                .lastName("Doe") // Unchanged
                .profileImage("https://example.com/profile.jpg") // Unchanged
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.updateCustomerProfile(eq(userId), any(UpdateCustomerRequest.class)))
                .thenReturn(updatedProfile);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/me/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Customer profile updated"))
                .andExpect(jsonPath("$.data.firstName").value("John Updated"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"));

        verify(userService, times(1)).updateCustomerProfile(eq(userId), any(UpdateCustomerRequest.class));
    }

    @Test
    @DisplayName("Should update customer profile with empty request body")
    void testUpdateCustomerProfile_EmptyRequest() throws Exception {
        // Given - Empty request (all fields null)
        UpdateCustomerRequest request = UpdateCustomerRequest.builder().build();

        when(userService.updateCustomerProfile(eq(userId), any(UpdateCustomerRequest.class)))
                .thenReturn(customerProfileResponse);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/me/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService, times(1)).updateCustomerProfile(eq(userId), any(UpdateCustomerRequest.class));
    }

    // ==================== UPDATE DRIVER PROFILE TESTS ====================

    @Test
    @DisplayName("Should update driver profile successfully")
    void testUpdateDriverProfile_Success() throws Exception {
        // Given
        UpdateDriverRequest request = UpdateDriverRequest.builder()
                .firstName("Jane Updated")
                .lastName("Smith Updated")
                .vehicleType(VehicleType.MOTORCYCLE)
                .vehicleNumber("XYZ789")
                .licenseNumber("LIC789")
                .isAvailable(false)
                .build();

        DriverProfileResponse updatedProfile = DriverProfileResponse.builder()
                .id(userId)
                .email("driver@example.com")
                .phone("+212612345678")
                .role(Role.DRIVER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .firstName("Jane Updated")
                .lastName("Smith Updated")
                .profileImage("https://example.com/profile.jpg")
                .vehicleType(VehicleType.MOTORCYCLE)
                .vehicleNumber("XYZ789")
                .licenseNumber("LIC789")
                .isAvailable(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.updateDriverProfile(eq(userId), any(UpdateDriverRequest.class)))
                .thenReturn(updatedProfile);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/me/driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Driver profile updated"))
                .andExpect(jsonPath("$.data.firstName").value("Jane Updated"))
                .andExpect(jsonPath("$.data.lastName").value("Smith Updated"))
                .andExpect(jsonPath("$.data.vehicleType").value("MOTORCYCLE"))
                .andExpect(jsonPath("$.data.vehicleNumber").value("XYZ789"))
                .andExpect(jsonPath("$.data.licenseNumber").value("LIC789"))
                .andExpect(jsonPath("$.data.isAvailable").value(false));

        verify(userService, times(1)).updateDriverProfile(eq(userId), any(UpdateDriverRequest.class));
    }

    @Test
    @DisplayName("Should update driver profile with partial data")
    void testUpdateDriverProfile_PartialUpdate() throws Exception {
        // Given - Only updating availability
        UpdateDriverRequest request = UpdateDriverRequest.builder()
                .isAvailable(false)
                .build();

        DriverProfileResponse updatedProfile = DriverProfileResponse.builder()
                .id(userId)
                .email("driver@example.com")
                .phone("+212612345678")
                .role(Role.DRIVER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .firstName("Jane")
                .lastName("Smith")
                .profileImage("https://example.com/profile.jpg")
                .vehicleType(VehicleType.CAR)
                .vehicleNumber("ABC123")
                .licenseNumber("LIC456")
                .isAvailable(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.updateDriverProfile(eq(userId), any(UpdateDriverRequest.class)))
                .thenReturn(updatedProfile);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/me/driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Driver profile updated"))
                .andExpect(jsonPath("$.data.isAvailable").value(false));

        verify(userService, times(1)).updateDriverProfile(eq(userId), any(UpdateDriverRequest.class));
    }

    @Test
    @DisplayName("Should update driver profile changing vehicle type")
    void testUpdateDriverProfile_ChangeVehicleType() throws Exception {
        // Given
        UpdateDriverRequest request = UpdateDriverRequest.builder()
                .vehicleType(VehicleType.BIKE)
                .vehicleNumber("BIKE001")
                .build();

        DriverProfileResponse updatedProfile = DriverProfileResponse.builder()
                .id(userId)
                .email("driver@example.com")
                .phone("+212612345678")
                .role(Role.DRIVER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .firstName("Jane")
                .lastName("Smith")
                .profileImage("https://example.com/profile.jpg")
                .vehicleType(VehicleType.BIKE)
                .vehicleNumber("BIKE001")
                .licenseNumber("LIC456")
                .isAvailable(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.updateDriverProfile(eq(userId), any(UpdateDriverRequest.class)))
                .thenReturn(updatedProfile);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/me/driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleType").value("BIKE"))
                .andExpect(jsonPath("$.data.vehicleNumber").value("BIKE001"));

        verify(userService, times(1)).updateDriverProfile(eq(userId), any(UpdateDriverRequest.class));
    }

    @Test
    @DisplayName("Should update driver profile with empty request body")
    void testUpdateDriverProfile_EmptyRequest() throws Exception {
        // Given - Empty request (all fields null)
        UpdateDriverRequest request = UpdateDriverRequest.builder().build();

        when(userService.updateDriverProfile(eq(userId), any(UpdateDriverRequest.class)))
                .thenReturn(driverProfileResponse);

        // When & Then
        mockMvc.perform(patch("/api/v1/users/me/driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService, times(1)).updateDriverProfile(eq(userId), any(UpdateDriverRequest.class));
    }

    // ==================== UPLOAD PROFILE IMAGE TESTS ====================

    @Test
    @DisplayName("Should upload profile image successfully")
    void testUploadProfileImage_Success() throws Exception {
        // Given
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        String imageUrl = "https://example.com/uploads/profile-" + userId + ".jpg";
        when(userService.uploadProfileImage(eq(userId), any())).thenReturn(imageUrl);

        // When & Then
        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(image)
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Profile image uploaded"))
                .andExpect(jsonPath("$.data").value(imageUrl));

        verify(userService, times(1)).uploadProfileImage(eq(userId), any());
    }

    @Test
    @DisplayName("Should upload PNG profile image successfully")
    void testUploadProfileImage_PngFormat() throws Exception {
        // Given
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.png",
                "image/png",
                "test png image content".getBytes()
        );

        String imageUrl = "https://example.com/uploads/profile-" + userId + ".png";
        when(userService.uploadProfileImage(eq(userId), any())).thenReturn(imageUrl);

        // When & Then
        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(image)
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(imageUrl));

        verify(userService, times(1)).uploadProfileImage(eq(userId), any());
    }

    @Test
    @DisplayName("Should upload large profile image successfully")
    void testUploadProfileImage_LargeFile() throws Exception {
        // Given
        byte[] largeImageContent = new byte[5 * 1024 * 1024]; // 5MB
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "large-profile.jpg",
                "image/jpeg",
                largeImageContent
        );

        String imageUrl = "https://example.com/uploads/profile-" + userId + "-large.jpg";
        when(userService.uploadProfileImage(eq(userId), any())).thenReturn(imageUrl);

        // When & Then
        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(image)
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(imageUrl));

        verify(userService, times(1)).uploadProfileImage(eq(userId), any());
    }

    @Test
    @DisplayName("Should extract user ID from authentication correctly for image upload")
    void testUploadProfileImage_AuthenticationExtraction() throws Exception {
        // Given
        UUID testUserId = UUID.randomUUID();
        when(authentication.getName()).thenReturn(testUserId.toString());

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        String imageUrl = "https://example.com/uploads/profile-" + testUserId + ".jpg";
        when(userService.uploadProfileImage(eq(testUserId), any())).thenReturn(imageUrl);

        // When & Then
        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(image)
                        .principal(authentication))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userService, times(1)).uploadProfileImage(eq(testUserId), any());
    }

    // ==================== AUTHENTICATION TESTS ====================

    @Test
    @DisplayName("Should handle different user IDs correctly across endpoints")
    void testMultipleUserIds() throws Exception {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        Authentication auth1 = mock(Authentication.class);
        Authentication auth2 = mock(Authentication.class);
        when(auth1.getName()).thenReturn(userId1.toString());
        when(auth2.getName()).thenReturn(userId2.toString());

        when(userService.getProfile(userId1)).thenReturn(customerProfileResponse);
        when(userService.getProfile(userId2)).thenReturn(driverProfileResponse);

        // When & Then - First user
        mockMvc.perform(get("/api/v1/users/me")
                        .principal(auth1))
                .andExpect(status().isOk());

        verify(userService, times(1)).getProfile(userId1);

        // When & Then - Second user
        mockMvc.perform(get("/api/v1/users/me")
                        .principal(auth2))
                .andExpect(status().isOk());

        verify(userService, times(1)).getProfile(userId2);
    }
}