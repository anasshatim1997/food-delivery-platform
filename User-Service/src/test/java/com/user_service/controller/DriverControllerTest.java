package com.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.request.ToggleAvailabilityRequest;
import com.user_service.dto.response.DriverProfileResponse;
import com.user_service.entity.Driver;
import com.user_service.enums.DocumentType;
import com.user_service.repository.DriverRepository;
import com.user_service.service.IDriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    @Mock
    private IDriverService driverService;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DriverController driverController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;
    private UUID driverId;
    private Driver driver;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(driverController).build();
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();
        driverId = UUID.randomUUID();
        driver = new Driver();
        driver.setId(driverId);
    }

    @Test
    void updateLocation_whenDriverExists_returnsNoContent() throws Exception {
        when(authentication.getName()).thenReturn(userId.toString());
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        doNothing().when(driverService).updateLocation(eq(driverId), any(BigDecimal.class), any(BigDecimal.class));

        String requestBody = "{\"latitude\": 48.8566, \"longitude\": 2.3522}";

        mockMvc.perform(patch("/api/v1/drivers/me/location")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        verify(driverRepository).findByUserId(userId);
        verify(driverService).updateLocation(eq(driverId), eq(new BigDecimal("48.8566")), eq(new BigDecimal("2.3522")));
    }

    @Test
    void updateLocation_whenDriverNotFound_throwsRuntimeException() {
        when(authentication.getName()).thenReturn(userId.toString());
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.empty());

        String requestBody = "{\"latitude\": 48.8566, \"longitude\": 2.3522}";

        org.springframework.test.web.servlet.MvcResult result;
        try {
            mockMvc.perform(patch("/api/v1/drivers/me/location")
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError());
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("Driver not found");
        }

        verify(driverRepository).findByUserId(userId);
        verifyNoInteractions(driverService);
    }

    @Test
    void toggleAvailability_whenDriverExistsAndAvailableTrue_returnsOkWithProfile() throws Exception {
        DriverProfileResponse profileResponse = new DriverProfileResponse();
        ToggleAvailabilityRequest request = new ToggleAvailabilityRequest();
        request.setIsAvailable(true);

        when(authentication.getName()).thenReturn(userId.toString());
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(driverService.toggleAvailability(driverId, true)).thenReturn(profileResponse);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(driverRepository).findByUserId(userId);
        verify(driverService).toggleAvailability(driverId, true);
    }

    @Test
    void toggleAvailability_whenDriverExistsAndAvailableFalse_returnsOkWithProfile() throws Exception {
        DriverProfileResponse profileResponse = new DriverProfileResponse();
        ToggleAvailabilityRequest request = new ToggleAvailabilityRequest();
        request.setIsAvailable(false);

        when(authentication.getName()).thenReturn(userId.toString());
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(driverService.toggleAvailability(driverId, false)).thenReturn(profileResponse);

        mockMvc.perform(patch("/api/v1/drivers/me/availability")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(driverService).toggleAvailability(driverId, false);
    }

    @Test
    void toggleAvailability_whenDriverNotFound_throwsRuntimeException() {
        ToggleAvailabilityRequest request = new ToggleAvailabilityRequest();
        request.setIsAvailable(true);

        when(authentication.getName()).thenReturn(userId.toString());
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.empty());

        try {
            mockMvc.perform(patch("/api/v1/drivers/me/availability")
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("Driver not found");
        }

        verifyNoInteractions(driverService);
    }

    @Test
    void uploadDocument_withIdCard_returnsOkWithDocumentUrl() throws Exception {
        String documentUrl = "https://storage.example.com/documents/id-card.jpg";
        MockMultipartFile file = new MockMultipartFile("file", "id-card.jpg", MediaType.IMAGE_JPEG_VALUE, "image data".getBytes());

        when(authentication.getName()).thenReturn(userId.toString());
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(driverService.uploadVerificationDocument(driverId, DocumentType.ID_CARD, file)).thenReturn(documentUrl);

        mockMvc.perform(multipart("/api/v1/drivers/me/documents/ID_CARD")
                        .file(file)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentUrl").value(documentUrl));

        verify(driverService).uploadVerificationDocument(driverId, DocumentType.ID_CARD, file);
    }

    @Test
    void uploadDocument_withDriversLicense_returnsOkWithDocumentUrl() throws Exception {
        String documentUrl = "https://storage.example.com/documents/license.jpg";
        MockMultipartFile file = new MockMultipartFile("file", "license.jpg", MediaType.IMAGE_JPEG_VALUE, "image data".getBytes());

        when(authentication.getName()).thenReturn(userId.toString());
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(driverService.uploadVerificationDocument(driverId, DocumentType.DRIVERS_LICENSE, file)).thenReturn(documentUrl);

        mockMvc.perform(multipart("/api/v1/drivers/me/documents/DRIVERS_LICENSE")
                        .file(file)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentUrl").value(documentUrl));

        verify(driverService).uploadVerificationDocument(driverId, DocumentType.DRIVERS_LICENSE, file);
    }

    @Test
    void uploadDocument_withVehicleRegistration_returnsOkWithDocumentUrl() throws Exception {
        String documentUrl = "https://storage.example.com/documents/registration.pdf";
        MockMultipartFile file = new MockMultipartFile("file", "registration.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf data".getBytes());

        when(authentication.getName()).thenReturn(userId.toString());
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(driverService.uploadVerificationDocument(driverId, DocumentType.VEHICLE_REGISTRATION, file)).thenReturn(documentUrl);

        mockMvc.perform(multipart("/api/v1/drivers/me/documents/VEHICLE_REGISTRATION")
                        .file(file)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentUrl").value(documentUrl));

        verify(driverService).uploadVerificationDocument(driverId, DocumentType.VEHICLE_REGISTRATION, file);
    }

    @Test
    void uploadDocument_withInsurance_returnsOkWithDocumentUrl() throws Exception {
        String documentUrl = "https://storage.example.com/documents/insurance.pdf";
        MockMultipartFile file = new MockMultipartFile("file", "insurance.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf data".getBytes());

        when(authentication.getName()).thenReturn(userId.toString());
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(driverService.uploadVerificationDocument(driverId, DocumentType.INSURANCE, file)).thenReturn(documentUrl);

        mockMvc.perform(multipart("/api/v1/drivers/me/documents/INSURANCE")
                        .file(file)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentUrl").value(documentUrl));

        verify(driverService).uploadVerificationDocument(driverId, DocumentType.INSURANCE, file);
    }

    @Test
    void uploadDocument_whenDriverNotFound_throwsRuntimeException() {
        MockMultipartFile file = new MockMultipartFile("file", "id.jpg", MediaType.IMAGE_JPEG_VALUE, "image".getBytes());

        when(authentication.getName()).thenReturn(userId.toString());
        when(driverRepository.findByUserId(userId)).thenReturn(Optional.empty());

        try {
            mockMvc.perform(multipart("/api/v1/drivers/me/documents/ID_CARD")
                            .file(file)
                            .principal(authentication))
                    .andExpect(status().isInternalServerError());
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("Driver not found");
        }

        verifyNoInteractions(driverService);
    }
}