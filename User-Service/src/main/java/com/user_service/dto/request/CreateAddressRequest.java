package com.user_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAddressRequest {

    @NotBlank(message = "Label is required")
    @Pattern(regexp = "Home|Work|Other", message = "Label must be Home, Work, or Other")
    private String label;

    @NotBlank(message = "Street is required")
    @Size(max = 255)
    private String street;

    @NotBlank(message = "Building is required")
    @Size(max = 100)
    private String building;

    @Size(max = 50)
    private String floor;

    @Size(max = 50)
    private String apartment;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0",  message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0",  message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @Size(max = 500)
    private String deliveryInstructions;

    private boolean isDefault = false;
}