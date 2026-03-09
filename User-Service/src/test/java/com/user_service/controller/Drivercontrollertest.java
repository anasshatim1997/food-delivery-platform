package com.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.request.ToggleAvailabilityRequest;
import com.user_service.dto.response.DriverProfileResponse;
import com.user_service.entity.Driver;
import com.user_service.enums.DocumentType;
import com.user_service.repository.DriverRepository;
import com.user_service.security.AppSecurityConfig;
import com.user_service.service.IDriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DriverController.class)
@Import(AppSecurityConfig.class)
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @InjectMocks
    private IDriverService driverService;

    @InjectMocks
    private DriverRepository driverRepository;

    private UUID driverId;
    private Driver driver;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        driverId = UUID.randomUUID();

        driver = new Driver();
        driver.setId(driverId);
    }

    @Nested
    class UpdateLocation {

        @Test
        @WithMockUser(username = "#{userId.toString()}")
        void returnsNoContentOnSuccessfulLocationUpdate() throws Exception {
            UUID userIdLocal = UUID.randomUUID();
            Driver localDriver = new Driver();
            localDriver.setId(UUID.randomUUID());

            when(driverRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(localDriver));

            String body = """
                    {"latitude":33.5731,"longitude":-7.5898}
                    """;

            mockMvc.perform(patch("/api/v1/drivers/me/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(req -> {
                                req.setUserPrincipal(userIdLocal::toString);
                                return req;
                            }))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        void returnsNotFoundWhenDriverDoesNotExist() throws Exception {
            when(driverRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

            String body = """
                    {"latitude":33.5731,"longitude":-7.5898}
                    """;

            mockMvc.perform(patch("/api/v1/drivers/me/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    class ToggleAvailability {

        @Test
        @WithMockUser
        void returnsDriverProfileResponseOnToggleAvailability() throws Exception {
            DriverProfileResponse profile = DriverProfileResponse.builder()
                    .id(driverId)
                    .firstName("Ali")
                    .lastName("Baba")
                    .isAvailable(true)
                    .build();

            when(driverRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(driver));
            when(driverService.toggleAvailability(eq(driverId), eq(true))).thenReturn(profile);

            ToggleAvailabilityRequest request = new ToggleAvailabilityRequest();
            request.setIsAvailable(true);

            mockMvc.perform(patch("/api/v1/drivers/me/availability")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(driverId.toString()))
                    .andExpect(jsonPath("$.firstName").value("Ali"))
                    .andExpect(jsonPath("$.isAvailable").value(true));
        }

        @Test
        @WithMockUser
        void returnsNotFoundWhenDriverDoesNotExistOnToggle() throws Exception {
            when(driverRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

            ToggleAvailabilityRequest request = new ToggleAvailabilityRequest();
            request.setIsAvailable(false);

            mockMvc.perform(patch("/api/v1/drivers/me/availability")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(patch("/api/v1/drivers/me/availability")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class UploadDocument {

        @Test
        @WithMockUser
        void returnsDocumentUrlOnSuccessfulUpload() throws Exception {
            String expectedUrl = "http://localhost:8001/files/documents/file.jpg";

            when(driverRepository.findByUserId(any(UUID.class))).thenReturn(Optional.of(driver));
            when(driverService.uploadVerificationDocument(eq(driverId), eq(DocumentType.DRIVERS_LICENSE), any()))
                    .thenReturn(expectedUrl);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "license.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart("/api/v1/drivers/me/documents/DRIVER_LICENSE")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documentUrl").value(expectedUrl));
        }

        @Test
        @WithMockUser
        void returnsErrorWhenDriverNotFoundOnUpload() throws Exception {
            when(driverRepository.findByUserId(any(UUID.class))).thenReturn(Optional.empty());

            MockMultipartFile file = new MockMultipartFile(
                    "file", "license.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart("/api/v1/drivers/me/documents/DRIVER_LICENSE")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "license.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart("/api/v1/drivers/me/documents/DRIVER_LICENSE")
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }
}