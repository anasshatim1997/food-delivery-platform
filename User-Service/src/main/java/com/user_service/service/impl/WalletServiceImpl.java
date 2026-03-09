package com.user_service.service.impl;

import com.user_service.dto.request.TopUpRequest;
import com.user_service.dto.response.TransactionResponse;
import com.user_service.dto.response.WalletBalanceResponse;
import com.user_service.entity.Customer;
import com.user_service.entity.User;
import com.user_service.entity.WalletTransaction;
import com.user_service.exception.InsufficientBalanceException;
import com.user_service.exception.ResourceNotFoundException;
import com.user_service.repository.CustomerRepository;
import com.user_service.repository.UserRepository;
import com.user_service.repository.WalletTransactionRepository;
import com.user_service.service.IWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements IWalletService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalance(UUID userId) {
        User user = findUserOrThrow(userId);
        Customer customer = findCustomerOrThrow(userId);

        return WalletBalanceResponse.builder()
                .balance(customer.getWalletBalance())
                .currency("MAD")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(UUID userId, Pageable pageable) {
        findUserOrThrow(userId);

        return walletTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toTransactionResponse);
    }

    @Override
    @Transactional
    public WalletBalanceResponse topUp(UUID userId, TopUpRequest request) {
        User user = findUserOrThrow(userId);
        Customer customer = findCustomerOrThrow(userId);

        log.info("Processing top-up for user {} with payment method {}", userId, request.getPaymentMethodId());

        credit(userId, request.getAmount(), "Wallet top-up", "PAYMENT", null);

        customer.setWalletBalance(customer.getWalletBalance().add(request.getAmount()));
        customerRepository.save(customer);

        log.info("Top-up successful for user {}. New balance: {}", userId, customer.getWalletBalance());

        return WalletBalanceResponse.builder()
                .balance(customer.getWalletBalance())
                .currency("MAD")
                .build();
    }

    @Override
    @Transactional
    public void debit(UUID userId, BigDecimal amount, String description, String referenceType, UUID referenceId) {
        Customer customer = findCustomerOrThrow(userId);

        if (customer.getWalletBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient wallet balance");
        }

        customer.setWalletBalance(customer.getWalletBalance().subtract(amount));
        customerRepository.save(customer);

        WalletTransaction transaction = WalletTransaction.builder()
                .userId(userId)
                .amount(amount)
                .type(WalletTransaction.TransactionType.DEBIT)
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();

        walletTransactionRepository.save(transaction);

        log.info("Debited {} MAD from user {}. New balance: {}", amount, userId, customer.getWalletBalance());
    }

    @Override
    @Transactional
    public void credit(UUID userId, BigDecimal amount, String description, String referenceType, UUID referenceId) {
        Customer customer = findCustomerOrThrow(userId);

        customer.setWalletBalance(customer.getWalletBalance().add(amount));
        customerRepository.save(customer);

        WalletTransaction transaction = WalletTransaction.builder()
                .userId(userId)
                .amount(amount)
                .type(WalletTransaction.TransactionType.CREDIT)
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();

        walletTransactionRepository.save(transaction);

        log.info("Credited {} MAD to user {}. New balance: {}", amount, userId, customer.getWalletBalance());
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Customer findCustomerOrThrow(UUID userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
    }

    private TransactionResponse toTransactionResponse(WalletTransaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .description(transaction.getDescription())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}