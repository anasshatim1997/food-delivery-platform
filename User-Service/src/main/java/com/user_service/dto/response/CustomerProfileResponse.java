package com.user_service.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CustomerProfileResponse extends UserProfileResponse {
    private String firstName;
    private String lastName;
    private String profileImage;
    private BigDecimal walletBalance;
    private Integer totalOrders;
}