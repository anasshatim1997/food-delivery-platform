package com.user_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    private String id;

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
