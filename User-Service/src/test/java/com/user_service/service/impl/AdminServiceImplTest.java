package com.user_service.service.impl;

import com.user_service.dto.request.UpdateStatusRequest;
import com.user_service.dto.request.UpdateVerificationRequest;
import com.user_service.dto.response.UserResponse;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.enums.VerificationStatus;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.mapper.UserMapper;
import com.user_service.repository.DriverRepository;
import com.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Tests")
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    private UUID userId;
    private User user;
    private Driver driver;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("test@test.com")
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .profileCompleted(true)
                .createdAt(LocalDateTime.now())
                .build();

        driver = Driver.builder()
                .id(userId)
                .verificationStatus(VerificationStatus.PENDING)
                .isAvailable(false)
                .build();

        userResponse = UserResponse.builder()
                .id(userId)
                .email("test@test.com")
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("searchUsers Tests")
    class SearchUsersTests {

        @Test
        @DisplayName("Should search users with all filters")
        void searchUsers_allFilters() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(user));

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
            when(userMapper.toUserResponse(user)).thenReturn(userResponse);

            Page<UserResponse> result = adminService.searchUsers("test@test.com", Role.CUSTOMER, Status.ACTIVE, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(userResponse);
            verify(userRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should search users with no filters")
        void searchUsers_noFilters() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(user));

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
            when(userMapper.toUserResponse(user)).thenReturn(userResponse);

            Page<UserResponse> result = adminService.searchUsers(null, null, null, pageable);

            assertThat(result).isNotNull();
            verify(userRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should search users by email only")
        void searchUsers_emailOnly() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(user));

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
            when(userMapper.toUserResponse(user)).thenReturn(userResponse);

            Page<UserResponse> result = adminService.searchUsers("test", null, null, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should search users by role only")
        void searchUsers_roleOnly() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(user));

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
            when(userMapper.toUserResponse(user)).thenReturn(userResponse);

            Page<UserResponse> result = adminService.searchUsers(null, Role.CUSTOMER, null, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should search users by status only")
        void searchUsers_statusOnly() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(user));

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
            when(userMapper.toUserResponse(user)).thenReturn(userResponse);

            Page<UserResponse> result = adminService.searchUsers(null, null, Status.ACTIVE, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty page when no users match")
        void searchUsers_noMatches() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> emptyPage = new PageImpl<>(List.of());

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

            Page<UserResponse> result = adminService.searchUsers("nonexistent@test.com", null, null, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should handle case-insensitive email search")
        void searchUsers_caseInsensitiveEmail() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(user));

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
            when(userMapper.toUserResponse(user)).thenReturn(userResponse);

            Page<UserResponse> result = adminService.searchUsers("TEST@TEST.COM", null, null, pageable);

            assertThat(result).isNotNull();
            verify(userRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void searchUsers_pagination() {
            Pageable pageable = PageRequest.of(1, 10);
            Page<User> userPage = new PageImpl<>(List.of(user), pageable, 25);

            when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
            when(userMapper.toUserResponse(user)).thenReturn(userResponse);

            Page<UserResponse> result = adminService.searchUsers(null, null, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(25);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.getNumber()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should get user by ID successfully")
        void getUserById_success() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userMapper.toUserResponse(user)).thenReturn(userResponse);

            UserResponse result = adminService.getUserById(userId);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(userResponse);
            verify(userRepository).findById(userId);
            verify(userMapper).toUserResponse(user);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getUserById_notFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.getUserById(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository).findById(userId);
            verify(userMapper, never()).toUserResponse(any());
        }

        @Test
        @DisplayName("Should get driver user by ID")
        void getUserById_driver() {
            User driverUser = User.builder()
                    .id(userId)
                    .email("driver@test.com")
                    .role(Role.DRIVER)
                    .status(Status.ACTIVE)
                    .build();

            UserResponse driverResponse = UserResponse.builder()
                    .id(userId)
                    .email("driver@test.com")
                    .role(Role.DRIVER)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(driverUser));
            when(userMapper.toUserResponse(driverUser)).thenReturn(driverResponse);

            UserResponse result = adminService.getUserById(userId);

            assertThat(result.getRole()).isEqualTo(Role.DRIVER);
        }

        @Test
        @DisplayName("Should get admin user by ID")
        void getUserById_admin() {
            User adminUser = User.builder()
                    .id(userId)
                    .email("admin@test.com")
                    .role(Role.ADMIN)
                    .status(Status.ACTIVE)
                    .build();

            UserResponse adminResponse = UserResponse.builder()
                    .id(userId)
                    .email("admin@test.com")
                    .role(Role.ADMIN)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));
            when(userMapper.toUserResponse(adminUser)).thenReturn(adminResponse);

            UserResponse result = adminService.getUserById(userId);

            assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        }
    }

    @Nested
    @DisplayName("updateUserStatus Tests")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("Should update user status to SUSPENDED successfully")
        void updateUserStatus_suspend_success() {
            UpdateStatusRequest request = UpdateStatusRequest.builder()
                    .status(Status.SUSPENDED)
                    .reason("Violated terms of service")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            adminService.updateUserStatus(userId, request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            assertThat(userCaptor.getValue().getStatus()).isEqualTo(Status.SUSPENDED);
        }

        @Test
        @DisplayName("Should update user status to DELETED successfully")
        void updateUserStatus_delete_success() {
            UpdateStatusRequest request = UpdateStatusRequest.builder()
                    .status(Status.DELETED)
                    .reason("User requested account deletion")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            adminService.updateUserStatus(userId, request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            assertThat(userCaptor.getValue().getStatus()).isEqualTo(Status.DELETED);
        }

        @Test
        @DisplayName("Should update user status to ACTIVE successfully")
        void updateUserStatus_activate_success() {
            user.setStatus(Status.SUSPENDED);

            UpdateStatusRequest request = UpdateStatusRequest.builder()
                    .status(Status.ACTIVE)
                    .reason("Appeal approved")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            adminService.updateUserStatus(userId, request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            assertThat(userCaptor.getValue().getStatus()).isEqualTo(Status.ACTIVE);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void updateUserStatus_userNotFound() {
            UpdateStatusRequest request = UpdateStatusRequest.builder()
                    .status(Status.SUSPENDED)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateUserStatus(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update status without reason")
        void updateUserStatus_noReason() {
            UpdateStatusRequest request = UpdateStatusRequest.builder()
                    .status(Status.SUSPENDED)
                    .reason(null)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            adminService.updateUserStatus(userId, request);

            verify(userRepository).save(user);
        }
    }

    @Nested
    @DisplayName("updateDriverVerification Tests")
    class UpdateDriverVerificationTests {

        @Test
        @DisplayName("Should approve driver verification successfully")
        void updateDriverVerification_approve_success() {
            UpdateVerificationRequest request = UpdateVerificationRequest.builder()
                    .status(VerificationStatus.APPROVED)
                    .reason("All documents verified")
                    .build();

            when(driverRepository.findById(driver.getId())).thenReturn(Optional.of(driver));
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            adminService.updateDriverVerification(driver.getId(), request);

            ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);
            verify(driverRepository).save(driverCaptor.capture());

            assertThat(driverCaptor.getValue().getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        }

        @Test
        @DisplayName("Should reject driver verification successfully")
        void updateDriverVerification_reject_success() {
            UpdateVerificationRequest request = UpdateVerificationRequest.builder()
                    .status(VerificationStatus.REJECTED)
                    .reason("Invalid driver license")
                    .build();

            when(driverRepository.findById(driver.getId())).thenReturn(Optional.of(driver));
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            adminService.updateDriverVerification(driver.getId(), request);

            ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);
            verify(driverRepository).save(driverCaptor.capture());

            assertThat(driverCaptor.getValue().getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        }

        @Test
        @DisplayName("Should update verification from APPROVED back to PENDING")
        void updateDriverVerification_approvedToPending() {
            driver.setVerificationStatus(VerificationStatus.APPROVED);

            UpdateVerificationRequest request = UpdateVerificationRequest.builder()
                    .status(VerificationStatus.PENDING)
                    .reason("Re-verification required")
                    .build();

            when(driverRepository.findById(driver.getId())).thenReturn(Optional.of(driver));
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            adminService.updateDriverVerification(driver.getId(), request);

            ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);
            verify(driverRepository).save(driverCaptor.capture());

            assertThat(driverCaptor.getValue().getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw exception when driver not found")
        void updateDriverVerification_driverNotFound() {
            UpdateVerificationRequest request = UpdateVerificationRequest.builder()
                    .status(VerificationStatus.APPROVED)
                    .build();

            when(driverRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateDriverVerification(UUID.randomUUID(), request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Driver not found");

            verify(driverRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update verification without reason")
        void updateDriverVerification_noReason() {
            UpdateVerificationRequest request = UpdateVerificationRequest.builder()
                    .status(VerificationStatus.APPROVED)
                    .reason(null)
                    .build();

            when(driverRepository.findById(driver.getId())).thenReturn(Optional.of(driver));
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            adminService.updateDriverVerification(driver.getId(), request);

            verify(driverRepository).save(driver);
        }

        @Test
        @DisplayName("Should handle multiple status updates")
        void updateDriverVerification_multipleUpdates() {
            when(driverRepository.findById(driver.getId())).thenReturn(Optional.of(driver));
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            UpdateVerificationRequest request1 = UpdateVerificationRequest.builder()
                    .status(VerificationStatus.APPROVED)
                    .reason("Initial approval")
                    .build();

            adminService.updateDriverVerification(driver.getId(), request1);

            UpdateVerificationRequest request2 = UpdateVerificationRequest.builder()
                    .status(VerificationStatus.REJECTED)
                    .reason("Document expired")
                    .build();

            adminService.updateDriverVerification(driver.getId(), request2);

            verify(driverRepository, times(2)).save(driver);
        }
    }
}