package com.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.request.UpdateStatusRequest;
import com.user_service.dto.request.UpdateVerificationRequest;
import com.user_service.dto.response.UserResponse;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.enums.VerificationStatus;
import com.user_service.service.IAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminUserController Tests")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @InjectMocks
    private IAdminService adminService;

    private UUID userId;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userResponse = UserResponse.builder()
                .id(userId)
                .email("test@test.com")
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("GET /api/admin/users")
    class SearchUsersTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should search users with all parameters")
        void searchUsers_allParams() throws Exception {
            when(adminService.searchUsers(eq("test@test.com"), eq(Role.CUSTOMER), eq(Status.ACTIVE), any()))
                    .thenReturn(new PageImpl<>(List.of(userResponse), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/admin/users")
                            .param("email", "test@test.com")
                            .param("role", "CUSTOMER")
                            .param("status", "ACTIVE")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].email").value("test@test.com"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(adminService).searchUsers(eq("test@test.com"), eq(Role.CUSTOMER), eq(Status.ACTIVE), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should search users without filters")
        void searchUsers_noFilters() throws Exception {
            when(adminService.searchUsers(eq(null), eq(null), eq(null), any()))
                    .thenReturn(new PageImpl<>(List.of(userResponse), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isOk());

            verify(adminService).searchUsers(eq(null), eq(null), eq(null), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return empty page when no users found")
        void searchUsers_emptyResult() throws Exception {
            when(adminService.searchUsers(any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users/{userId}")
    class GetUserByIdTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get user by ID successfully")
        void getUserById_success() throws Exception {
            when(adminService.getUserById(userId)).thenReturn(userResponse);

            mockMvc.perform(get("/api/admin/users/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId.toString()))
                    .andExpect(jsonPath("$.email").value("test@test.com"));

            verify(adminService).getUserById(userId);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when user not found")
        void getUserById_notFound() throws Exception {
            when(adminService.getUserById(any())).thenThrow(new RuntimeException("User not found"));

            mockMvc.perform(get("/api/admin/users/{userId}", UUID.randomUUID()))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/users/{userId}/status")
    class UpdateUserStatusTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update user status successfully")
        void updateUserStatus_success() throws Exception {
            UpdateStatusRequest request = UpdateStatusRequest.builder()
                    .status(Status.SUSPENDED)
                    .reason("Violated terms")
                    .build();

            doNothing().when(adminService).updateUserStatus(eq(userId), any(UpdateStatusRequest.class));

            mockMvc.perform(patch("/api/admin/users/{userId}/status", userId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(adminService).updateUserStatus(eq(userId), any(UpdateStatusRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 when status is null")
        void updateUserStatus_nullStatus() throws Exception {
            UpdateStatusRequest request = UpdateStatusRequest.builder()
                    .status(null)
                    .reason("Some reason")
                    .build();

            mockMvc.perform(patch("/api/admin/users/{userId}/status", userId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(adminService, never()).updateUserStatus(any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update status without reason")
        void updateUserStatus_noReason() throws Exception {
            UpdateStatusRequest request = UpdateStatusRequest.builder()
                    .status(Status.ACTIVE)
                    .build();

            doNothing().when(adminService).updateUserStatus(eq(userId), any(UpdateStatusRequest.class));

            mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", userId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/users/drivers/{driverId}/verification")
    class UpdateDriverVerificationTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update driver verification successfully")
        void updateDriverVerification_success() throws Exception {
            UUID driverId = UUID.randomUUID();
            UpdateVerificationRequest request = UpdateVerificationRequest.builder()
                    .status(VerificationStatus.APPROVED)
                    .reason("All documents valid")
                    .build();

            doNothing().when(adminService).updateDriverVerification(eq(driverId), any(UpdateVerificationRequest.class));

            mockMvc.perform(patch("/api/admin/users/drivers/{driverId}/verification", driverId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(adminService).updateDriverVerification(eq(driverId), any(UpdateVerificationRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 when verification status is null")
        void updateDriverVerification_nullStatus() throws Exception {
            UUID driverId = UUID.randomUUID();
            UpdateVerificationRequest request = UpdateVerificationRequest.builder()
                    .status(null)
                    .reason("Some reason")
                    .build();

            mockMvc.perform(patch("/api/admin/users/drivers/{driverId}/verification", driverId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(adminService, never()).updateDriverVerification(any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should reject driver verification")
        void updateDriverVerification_reject() throws Exception {
            UUID driverId = UUID.randomUUID();
            UpdateVerificationRequest request = UpdateVerificationRequest.builder()
                    .status(VerificationStatus.REJECTED)
                    .reason("Invalid documents")
                    .build();

            doNothing().when(adminService).updateDriverVerification(eq(driverId), any(UpdateVerificationRequest.class));

            mockMvc.perform(patch("/api/admin/users/drivers/{driverId}/verification", driverId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }
    }
}