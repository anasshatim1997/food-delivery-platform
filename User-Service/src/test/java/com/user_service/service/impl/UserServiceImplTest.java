package com.user_service.service.impl;

import com.user_service.dto.request.UpdateCustomerRequest;
import com.user_service.dto.request.UpdateDriverRequest;
import com.user_service.dto.response.CustomerProfileResponse;
import com.user_service.dto.response.DriverProfileResponse;
import com.user_service.dto.response.UserProfileResponse;
import com.user_service.entity.Customer;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.Status;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.mapper.CustomerMapper;
import com.user_service.mapper.DriverMapper;
import com.user_service.mapper.UserMapper;
import com.user_service.repository.CustomerRepository;
import com.user_service.repository.DriverRepository;
import com.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private UserMapper userMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private DriverMapper driverMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User customerUser;
    private User driverUser;
    private Customer customer;
    private Driver driver;

    private CustomerProfileResponse customerProfileResponse;
    private DriverProfileResponse driverProfileResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        customerUser = User.builder()
                .id(userId)
                .email("customer@mail.com")
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .build();

        driverUser = User.builder()
                .id(userId)
                .email("driver@mail.com")
                .role(Role.DRIVER)
                .status(Status.ACTIVE)
                .build();

        customer = new Customer();
        customer.setId(userId);

        driver = new Driver();

        customerProfileResponse = new CustomerProfileResponse();
        driverProfileResponse   = new DriverProfileResponse();
    }

    // =========================================================================
    // getProfile
    // =========================================================================

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("returns customer profile response for a CUSTOMER role user")
        void getProfile_customer() {
            // --- Arrange ---
            when(userRepository.findById(userId)).thenReturn(Optional.of(customerUser));
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));
            when(userMapper.toCustomerProfileResponse(customerUser, customer)).thenReturn(customerProfileResponse);

            // --- Act ---
            UserProfileResponse response = userService.getProfile(userId);

            // --- Assert ---
            assertThat(response).isEqualTo(customerProfileResponse);
            verify(userMapper).toCustomerProfileResponse(customerUser, customer);
        }

        @Test
        @DisplayName("returns driver profile response for a DRIVER role user")
        void getProfile_driver() {
            // --- Arrange ---
            when(userRepository.findById(userId)).thenReturn(Optional.of(driverUser));
            when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
            when(userMapper.toDriverProfileResponse(driverUser, driver)).thenReturn(driverProfileResponse);

            // --- Act ---
            UserProfileResponse response = userService.getProfile(userId);

            // --- Assert ---
            assertThat(response).isEqualTo(driverProfileResponse);
            verify(userMapper).toDriverProfileResponse(driverUser, driver);
        }

        @Test
        @DisplayName("throws when user does not exist")
        void getProfile_userNotFound() {
            // --- Arrange ---
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // --- Act & Assert ---
            assertThatThrownBy(() -> userService.getProfile(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws when CUSTOMER user has no customer profile")
        void getProfile_customerProfileMissing() {
            // --- Arrange ---
            when(userRepository.findById(userId)).thenReturn(Optional.of(customerUser));
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // --- Act & Assert ---
            assertThatThrownBy(() -> userService.getProfile(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // updateCustomerProfile
    // =========================================================================

    @Nested
    @DisplayName("updateCustomerProfile")
    class UpdateCustomerProfile {

        @Test
        @DisplayName("applies mapper updates, saves customer, and returns updated profile")
        void updateCustomerProfile_success() {
            // --- Arrange ---
            UpdateCustomerRequest request = new UpdateCustomerRequest();

            when(userRepository.findById(userId)).thenReturn(Optional.of(customerUser));
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));
            when(userMapper.toCustomerProfileResponse(customerUser, customer)).thenReturn(customerProfileResponse);

            // --- Act ---
            UserProfileResponse response = userService.updateCustomerProfile(userId, request);

            // --- Assert ---
            verify(customerMapper).updateCustomer(request, customer);
            verify(customerRepository).save(customer);
            assertThat(response).isEqualTo(customerProfileResponse);
        }

        @Test
        @DisplayName("throws when customer profile does not exist")
        void updateCustomerProfile_notFound() {
            // --- Arrange ---
            when(userRepository.findById(userId)).thenReturn(Optional.of(customerUser));
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // --- Act & Assert ---
            assertThatThrownBy(() -> userService.updateCustomerProfile(userId, new UpdateCustomerRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // updateDriverProfile
    // =========================================================================

    @Nested
    @DisplayName("updateDriverProfile")
    class UpdateDriverProfile {

        @Test
        @DisplayName("applies mapper updates, saves driver, and returns updated profile")
        void updateDriverProfile_success() {
            // --- Arrange ---
            UpdateDriverRequest request = new UpdateDriverRequest();

            when(userRepository.findById(userId)).thenReturn(Optional.of(driverUser));
            when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
            when(userMapper.toDriverProfileResponse(driverUser, driver)).thenReturn(driverProfileResponse);

            // --- Act ---
            UserProfileResponse response = userService.updateDriverProfile(userId, request);

            // --- Assert ---
            verify(driverMapper).updateDriver(request, driver);
            verify(driverRepository).save(driver);
            assertThat(response).isEqualTo(driverProfileResponse);
        }

        @Test
        @DisplayName("throws when driver profile does not exist")
        void updateDriverProfile_notFound() {
            // --- Arrange ---
            when(userRepository.findById(userId)).thenReturn(Optional.of(driverUser));
            when(driverRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // --- Act & Assert ---
            assertThatThrownBy(() -> userService.updateDriverProfile(userId, new UpdateDriverRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}