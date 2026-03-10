package com.user_service.controller;

import com.user_service.dto.request.UpdateStatusRequest;
import com.user_service.dto.request.UpdateVerificationRequest;
import com.user_service.dto.response.UserResponse;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.service.IAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private IAdminService adminService;

    @InjectMocks
    private AdminUserController adminUserController;

    private UUID userId;
    private UUID driverId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        driverId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);
    }

    @Test
    void searchUsers_withNoFilters_returnsAllUsers() {
        UserResponse user = UserResponse.builder().build();
        Page<UserResponse> page = new PageImpl<>(List.of(user));
        when(adminService.searchUsers(null, null, null, pageable)).thenReturn(page);

        ResponseEntity<Page<UserResponse>> response = adminUserController.searchUsers(null, null, null, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(adminService).searchUsers(null, null, null, pageable);
    }

    @Test
    void searchUsers_withEmailFilter_returnsMatchingUsers() {
        String email = "test@example.com";
        Page<UserResponse> page = new PageImpl<>(List.of(UserResponse.builder().build()));
        when(adminService.searchUsers(email, null, null, pageable)).thenReturn(page);

        ResponseEntity<Page<UserResponse>> response = adminUserController.searchUsers(email, null, null, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(adminService).searchUsers(email, null, null, pageable);
    }

    @Test
    void searchUsers_withRoleFilter_returnsMatchingUsers() {
        Page<UserResponse> page = new PageImpl<>(List.of(UserResponse.builder().build()));
        when(adminService.searchUsers(null, Role.DRIVER, null, pageable)).thenReturn(page);

        ResponseEntity<Page<UserResponse>> response = adminUserController.searchUsers(null, Role.DRIVER, null, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminService).searchUsers(null, Role.DRIVER, null, pageable);
    }

    @Test
    void searchUsers_withStatusFilter_returnsMatchingUsers() {
        Page<UserResponse> page = new PageImpl<>(List.of(UserResponse.builder().build()));
        when(adminService.searchUsers(null, null, Status.ACTIVE, pageable)).thenReturn(page);

        ResponseEntity<Page<UserResponse>> response = adminUserController.searchUsers(null, null, Status.ACTIVE, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminService).searchUsers(null, null, Status.ACTIVE, pageable);
    }

    @Test
    void searchUsers_withAllFilters_returnsMatchingUsers() {
        String email = "driver@example.com";
        Page<UserResponse> page = new PageImpl<>(List.of(UserResponse.builder().build()));
        when(adminService.searchUsers(email, Role.DRIVER, Status.ACTIVE, pageable)).thenReturn(page);

        ResponseEntity<Page<UserResponse>> response = adminUserController.searchUsers(email, Role.DRIVER, Status.ACTIVE, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(adminService).searchUsers(email, Role.DRIVER, Status.ACTIVE, pageable);
    }

    @Test
    void searchUsers_returnsEmptyPage_whenNoMatch() {
        Page<UserResponse> emptyPage = new PageImpl<>(List.of());
        when(adminService.searchUsers(null, null, null, pageable)).thenReturn(emptyPage);

        ResponseEntity<Page<UserResponse>> response = adminUserController.searchUsers(null, null, null, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void getUserById_returnsUserResponse() {
        UserResponse userResponse = UserResponse.builder().id(userId).email("test@example.com").build();
        when(adminService.getUserById(userId)).thenReturn(userResponse);

        ResponseEntity<UserResponse> response = adminUserController.getUserById(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(userResponse);
        verify(adminService).getUserById(userId);
    }

    @Test
    void getUserById_delegatesToAdminService() {
        when(adminService.getUserById(userId)).thenReturn(UserResponse.builder().build());

        adminUserController.getUserById(userId);

        verify(adminService, times(1)).getUserById(userId);
    }

    @Test
    void updateUserStatus_returnsNoContent() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        doNothing().when(adminService).updateUserStatus(userId, request);

        ResponseEntity<Void> response = adminUserController.updateUserStatus(userId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(adminService).updateUserStatus(userId, request);
    }

    @Test
    void updateUserStatus_delegatesToAdminService() {
        UpdateStatusRequest request = new UpdateStatusRequest();

        adminUserController.updateUserStatus(userId, request);

        verify(adminService, times(1)).updateUserStatus(userId, request);
    }

    @Test
    void updateDriverVerification_returnsNoContent() {
        UpdateVerificationRequest request = new UpdateVerificationRequest();
        doNothing().when(adminService).updateDriverVerification(driverId, request);

        ResponseEntity<Void> response = adminUserController.updateDriverVerification(driverId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(adminService).updateDriverVerification(driverId, request);
    }

    @Test
    void updateDriverVerification_delegatesToAdminService() {
        UpdateVerificationRequest request = new UpdateVerificationRequest();

        adminUserController.updateDriverVerification(driverId, request);

        verify(adminService, times(1)).updateDriverVerification(driverId, request);
    }
}