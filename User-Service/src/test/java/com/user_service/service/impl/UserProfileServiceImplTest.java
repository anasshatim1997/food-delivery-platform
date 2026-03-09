package com.user_service.service.impl;

import com.user_service.dto.request.CompleteCustomerProfileRequest;
import com.user_service.dto.request.CompleteDriverProfileRequest;
import com.user_service.entity.Customer;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.Role;
import com.user_service.enums.VehicleType;
import com.user_service.enums.VerificationStatus;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.repository.CustomerRepository;
import com.user_service.repository.DriverRepository;
import com.user_service.service.IStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private IStorageService IStorageService;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private UUID userId;
    private User user;
    private Customer customer;
    private Driver driver;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("user@mail.com")
                .role(Role.CUSTOMER)
                .build();

        customer = new Customer();
        customer.setId(userId);
        customer.setFirstName("Alice");
        customer.setLastName("Smith");

        driver = new Driver();
    }

    // =========================================================================
    // createCustomerProfile
    // =========================================================================

    @Nested
    @DisplayName("createCustomerProfile")
    class CreateCustomerProfile {

        @Test
        @DisplayName("creates and saves a customer profile with zero wallet and order count")
        void createCustomerProfile_success() {
            // --- Arrange ---
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            // --- Act ---
            userProfileService.createCustomerProfile(user, "Alice", "Smith");

            // --- Assert ---
            verify(customerRepository).save(argThat(c ->
                    c.getFirstName().equals("Alice") &&
                            c.getLastName().equals("Smith") &&
                            c.getWalletBalance().compareTo(BigDecimal.ZERO) == 0 &&
                            c.getTotalOrders() == 0
            ));
        }
    }

    // =========================================================================
    // createDriverProfile
    // =========================================================================

    @Nested
    @DisplayName("createDriverProfile")
    class CreateDriverProfile {

        @Test
        @DisplayName("creates and saves a driver profile with PENDING verification status")
        void createDriverProfile_success() {
            // --- Arrange ---
            when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

            // --- Act ---
            userProfileService.createDriverProfile(user, "Bob", "Jones", "CAR", "ABC-123", "LIC-456");

            // --- Assert ---
            verify(driverRepository).save(argThat(d ->
                    d.getFirstName().equals("Bob") &&
                            d.getVehicleType() == VehicleType.CAR &&
                            d.getVehicleNumber().equals("ABC-123") &&
                            d.getLicenseNumber().equals("LIC-456") &&
                            d.getVerificationStatus() == VerificationStatus.PENDING &&
                            !d.getIsAvailable()
            ));
        }
    }

    // =========================================================================
    // completeCustomerProfile
    // =========================================================================

    @Nested
    @DisplayName("completeCustomerProfile")
    class CompleteCustomerProfile {

        @Test
        @DisplayName("updates existing customer profile fields")
        void completeCustomerProfile_existingCustomer() {
            // --- Arrange ---
            CompleteCustomerProfileRequest request = new CompleteCustomerProfileRequest();
            request.setFirstName("Alice");
            request.setLastName("Updated");

            when(customerRepository.findById(userId)).thenReturn(Optional.of(customer));

            // --- Act ---
            userProfileService.completeCustomerProfile(userId, user, request);

            // --- Assert ---
            assertThat(customer.getFirstName()).isEqualTo("Alice");
            assertThat(customer.getLastName()).isEqualTo("Updated");
            verify(customerRepository).save(customer);
        }

        @Test
        @DisplayName("creates a new customer profile when none exists yet")
        void completeCustomerProfile_createsNewIfMissing() {
            // --- Arrange ---
            CompleteCustomerProfileRequest request = new CompleteCustomerProfileRequest();
            request.setFirstName("New");
            request.setLastName("Customer");

            when(customerRepository.findById(userId)).thenReturn(Optional.empty());
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            // --- Act ---
            userProfileService.completeCustomerProfile(userId, user, request);

            // --- Assert ---
            verify(customerRepository).save(argThat(c ->
                    c.getFirstName().equals("New") && c.getLastName().equals("Customer")
            ));
        }
    }

    // =========================================================================
    // completeDriverProfile
    // =========================================================================

    @Nested
    @DisplayName("completeDriverProfile")
    class CompleteDriverProfile {

        @Test
        @DisplayName("updates existing driver profile and sets verification status to PENDING")
        void completeDriverProfile_existingDriver() {
            // --- Arrange ---
            CompleteDriverProfileRequest request = new CompleteDriverProfileRequest();
            request.setFirstName("Bob");
            request.setLastName("Jones");
            request.setVehicleType(VehicleType.BIKE);
            request.setVehicleNumber("XYZ-999");
            request.setLicenseNumber("LIC-999");

            when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));

            // --- Act ---
            userProfileService.completeDriverProfile(userId, user, request);

            // --- Assert ---
            assertThat(driver.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
            assertThat(driver.getVehicleType()).isEqualTo(VehicleType.BIKE);
            verify(driverRepository).save(driver);
        }
    }

    // =========================================================================
    // uploadProfileImage (customer)
    // =========================================================================

    @Nested
    @DisplayName("uploadProfileImage (customer)")
    class UploadProfileImage {

        @Test
        @DisplayName("deletes old image, uploads new one, and saves updated customer")
        void uploadProfileImage_replacesOldImage() {
            // --- Arrange ---
            customer.setProfileImage("http://example.com/old.jpg");
            MockMultipartFile file = new MockMultipartFile("image", "new.jpg", "image/jpeg", new byte[]{1, 2, 3});

            when(customerRepository.findById(userId)).thenReturn(Optional.of(customer));
            when(IStorageService.uploadFile(file, "customers/profiles")).thenReturn("http://example.com/new.jpg");

            // --- Act ---
            String result = userProfileService.uploadProfileImage(userId, file);

            // --- Assert ---
            assertThat(result).isEqualTo("http://example.com/new.jpg");
            verify(IStorageService).deleteFile("http://example.com/old.jpg");
            assertThat(customer.getProfileImage()).isEqualTo("http://example.com/new.jpg");
            verify(customerRepository).save(customer);
        }

        @Test
        @DisplayName("skips delete when customer has no existing profile image")
        void uploadProfileImage_noOldImage() {
            // --- Arrange ---
            customer.setProfileImage(null);
            MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1});

            when(customerRepository.findById(userId)).thenReturn(Optional.of(customer));
            when(IStorageService.uploadFile(any(), any())).thenReturn("http://example.com/photo.png");

            // --- Act ---
            userProfileService.uploadProfileImage(userId, file);

            // --- Assert ---
            verify(IStorageService, never()).deleteFile(any());
        }

        @Test
        @DisplayName("throws when customer is not found")
        void uploadProfileImage_customerNotFound() {
            // --- Arrange ---
            when(customerRepository.findById(userId)).thenReturn(Optional.empty());
            MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1});

            // --- Act & Assert ---
            assertThatThrownBy(() -> userProfileService.uploadProfileImage(userId, file))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // uploadDriverProfileImage
    // =========================================================================

    @Nested
    @DisplayName("uploadDriverProfileImage")
    class UploadDriverProfileImage {

        @Test
        @DisplayName("uploads new driver profile image and saves")
        void uploadDriverProfileImage_success() {
            // --- Arrange ---
            driver.setProfileImage(null);
            MockMultipartFile file = new MockMultipartFile("image", "driver.jpg", "image/jpeg", new byte[]{1});

            when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
            when(IStorageService.uploadFile(file, "drivers/profiles")).thenReturn("http://example.com/driver.jpg");

            // --- Act ---
            String result = userProfileService.uploadDriverProfileImage(userId, file);

            // --- Assert ---
            assertThat(result).isEqualTo("http://example.com/driver.jpg");
            assertThat(driver.getProfileImage()).isEqualTo("http://example.com/driver.jpg");
            verify(driverRepository).save(driver);
        }

        @Test
        @DisplayName("throws when driver is not found")
        void uploadDriverProfileImage_notFound() {
            // --- Arrange ---
            when(driverRepository.findByUserId(userId)).thenReturn(Optional.empty());
            MockMultipartFile file = new MockMultipartFile("image", "driver.jpg", "image/jpeg", new byte[]{1});

            // --- Act & Assert ---
            assertThatThrownBy(() -> userProfileService.uploadDriverProfileImage(userId, file))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // uploadDriverLicenseImage
    // =========================================================================

    @Nested
    @DisplayName("uploadDriverLicenseImage")
    class UploadDriverLicenseImage {

        @Test
        @DisplayName("replaces old license image, uploads new one, and saves")
        void uploadDriverLicenseImage_replacesOld() {
            // --- Arrange ---
            driver.setLicenseImage("http://example.com/old_license.jpg");
            MockMultipartFile file = new MockMultipartFile("image", "license.jpg", "image/jpeg", new byte[]{1});

            when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
            when(IStorageService.uploadFile(file, "drivers/licenses")).thenReturn("http://example.com/new_license.jpg");

            // --- Act ---
            String result = userProfileService.uploadDriverLicenseImage(userId, file);

            // --- Assert ---
            assertThat(result).isEqualTo("http://example.com/new_license.jpg");
            verify(IStorageService).deleteFile("http://example.com/old_license.jpg");
            assertThat(driver.getLicenseImage()).isEqualTo("http://example.com/new_license.jpg");
        }
    }
}