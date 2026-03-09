package com.user_service.service;

import com.user_service.dto.request.TopUpRequest;
import com.user_service.dto.response.TransactionResponse;
import com.user_service.dto.response.WalletBalanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface IWalletService {

    WalletBalanceResponse getBalance(UUID userId);

    Page<TransactionResponse> getTransactions(UUID userId, Pageable pageable);

    WalletBalanceResponse topUp(UUID userId, TopUpRequest request);

    void debit(UUID userId, BigDecimal amount, String description, String referenceType, UUID referenceId);

    void credit(UUID userId, BigDecimal amount, String description, String referenceType, UUID referenceId);
}