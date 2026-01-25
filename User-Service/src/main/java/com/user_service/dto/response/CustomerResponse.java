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
public class CustomerResponse {

    private String id;

    private String email;

    private String phone;

    private String firstName;

    private String lastName;

    private String profileImage;

    private BigDecimal walletBalance;

    private Integer totalOrders;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}