package com.user_service.dto.response;

import com.user_service.entity.WalletTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private BigDecimal amount;
    private WalletTransaction.TransactionType type;
    private String description;
    private String referenceType;
    private UUID referenceId;
    private LocalDateTime createdAt;
}