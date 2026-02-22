package com.user_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AddressResponse {

    private UUID id;
    private UUID customerId;
    private String label;
    private String street;
    private String building;
    private String floor;
    private String apartment;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String deliveryInstructions;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}