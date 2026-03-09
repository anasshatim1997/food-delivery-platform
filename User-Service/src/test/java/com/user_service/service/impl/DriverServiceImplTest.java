package com.user_service.service.impl;

import com.user_service.dto.response.DriverProfileResponse;
import com.user_service.entity.Driver;
import com.user_service.entity.User;
import com.user_service.enums.DocumentType;
import com.user_service.enums.VerificationStatus;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.mapper.DriverMapper;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriverService Tests")
class DriverServiceImplTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverMapper driverMapper;

    @Mock
    private IStorageService storageService;

    @InjectMocks
    private DriverServiceImpl driverService;

    private UUID driverId;
    private Driver driver;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();

        User user = User.builder()
                .id(driverId)
                .build();

        driver = Driver.builder()
                .id(driverId)
                .user(user)
                .verificationStatus(VerificationStatus.APPROVED)
                .isAvailable(false)
                .verificationDocuments(new HashMap<>())
                .build();
    }

    @Nested
    @DisplayName("updateLocation Tests")
    class UpdateLocationTests {

        @Test
        @DisplayName("Should update driver location successfully")
        void updateLocation_success() {
            BigDecimal lat = BigDecimal.valueOf(33.5731);
            BigDecimal lng = BigDecimal.valueOf(-7.5898);

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            driverService.updateLocation(driverId, lat, lng);

            assertThat(driver.getCurrentLat()).isEqualByComparingTo(lat);
            assertThat(driver.getCurrentLng()).isEqualByComparingTo(lng);
            verify(driverRepository).save(driver);
        }

        @Test
        @DisplayName("Should throw exception when driver not found")
        void updateLocation_driverNotFound() {
            when(driverRepository.findById(driverId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> driverService.updateLocation(driverId, BigDecimal.ZERO, BigDecimal.ZERO))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Driver not found");
        }
    }

    @Nested
    @DisplayName("toggleAvailability Tests")
    class ToggleAvailabilityTests {

        @Test
        @DisplayName("Should toggle availability to online successfully")
        void toggleAvailability_online_success() {
            DriverProfileResponse expected = new DriverProfileResponse();

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);
            when(driverMapper.toDriverProfileResponse(driver)).thenReturn(expected);

            DriverProfileResponse result = driverService.toggleAvailability(driverId, true);

            assertThat(driver.getIsAvailable()).isTrue();
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should throw exception when driver not verified")
        void toggleAvailability_notVerified() {
            driver.setVerificationStatus(VerificationStatus.PENDING);

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));

            assertThatThrownBy(() -> driverService.toggleAvailability(driverId, true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Driver must be verified before going online");
        }

        @Test
        @DisplayName("Should toggle availability to offline successfully")
        void toggleAvailability_offline_success() {
            driver.setIsAvailable(true);
            DriverProfileResponse expected = new DriverProfileResponse();

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);
            when(driverMapper.toDriverProfileResponse(driver)).thenReturn(expected);

            DriverProfileResponse result = driverService.toggleAvailability(driverId, false);

            assertThat(driver.getIsAvailable()).isFalse();
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("uploadVerificationDocument Tests")
    class UploadVerificationDocumentTests {

        @Test
        @DisplayName("Should upload verification document successfully")
        void uploadDocument_success() {
            MultipartFile file = mock(MultipartFile.class);
            String expectedUrl = "http://storage.com/documents/id-card.jpg";

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
            when(storageService.uploadFile(file, "driver-documents")).thenReturn(expectedUrl);
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            String result = driverService.uploadVerificationDocument(driverId, DocumentType.ID_CARD, file);

            assertThat(result).isEqualTo(expectedUrl);
            assertThat(driver.getVerificationDocuments()).containsEntry("ID_CARD", expectedUrl);
            verify(driverRepository).save(driver);
        }

        @Test
        @DisplayName("Should replace existing document")
        void uploadDocument_replaceExisting() {
            MultipartFile file = mock(MultipartFile.class);
            String oldUrl = "http://storage.com/old-id.jpg";
            String newUrl = "http://storage.com/new-id.jpg";

            driver.getVerificationDocuments().put("ID_CARD", oldUrl);

            when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
            when(storageService.uploadFile(file, "driver-documents")).thenReturn(newUrl);
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            String result = driverService.uploadVerificationDocument(driverId, DocumentType.ID_CARD, file);

            assertThat(result).isEqualTo(newUrl);
            assertThat(driver.getVerificationDocuments().get("ID_CARD")).isEqualTo(newUrl);
        }
    }
}