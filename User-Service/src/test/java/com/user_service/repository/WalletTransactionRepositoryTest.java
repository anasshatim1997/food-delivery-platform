package com.user_service.repository;

import com.user_service.entity.WalletTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class WalletTransactionRepositoryTest {

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        walletTransactionRepository.deleteAll();
    }

    private WalletTransaction buildTransaction(UUID uid, BigDecimal amount, WalletTransaction.TransactionType type, String description) {
        return WalletTransaction.builder()
                .userId(uid)
                .amount(amount)
                .type(type)
                .description(description)
                .referenceType("ORDER")
                .referenceId(UUID.randomUUID())
                .build();
    }

    @Nested
    class FindByUserIdOrderByCreatedAtDesc {

        @Test
        void returnsTransactionsForCorrectUser() {
            walletTransactionRepository.save(buildTransaction(userId, BigDecimal.valueOf(50), WalletTransaction.TransactionType.CREDIT, "Top-up"));
            walletTransactionRepository.save(buildTransaction(userId, BigDecimal.valueOf(20), WalletTransaction.TransactionType.DEBIT, "Order payment"));

            Page<WalletTransaction> result = walletTransactionRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            result.getContent().forEach(tx -> assertThat(tx.getUserId()).isEqualTo(userId));
        }

        @Test
        void returnsEmptyPageForUserWithNoTransactions() {
            UUID otherUser = UUID.randomUUID();
            walletTransactionRepository.save(buildTransaction(otherUser, BigDecimal.TEN, WalletTransaction.TransactionType.CREDIT, "Top-up"));

            Page<WalletTransaction> result = walletTransactionRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void returnsTransactionsInDescendingOrderByCreatedAt() throws InterruptedException {
            WalletTransaction first = walletTransactionRepository.save(
                    buildTransaction(userId, BigDecimal.valueOf(10), WalletTransaction.TransactionType.CREDIT, "First")
            );
            Thread.sleep(10);
            WalletTransaction second = walletTransactionRepository.save(
                    buildTransaction(userId, BigDecimal.valueOf(20), WalletTransaction.TransactionType.CREDIT, "Second")
            );

            Page<WalletTransaction> result = walletTransactionRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

            assertThat(result.getContent().get(0).getId()).isEqualTo(second.getId());
            assertThat(result.getContent().get(1).getId()).isEqualTo(first.getId());
        }

        @Test
        void respectsPaginationLimits() {
            for (int i = 0; i < 5; i++) {
                walletTransactionRepository.save(
                        buildTransaction(userId, BigDecimal.valueOf(i + 1), WalletTransaction.TransactionType.CREDIT, "tx-" + i)
                );
            }

            Page<WalletTransaction> result = walletTransactionRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 2));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }

        @Test
        void returnsBothCreditAndDebitTransactions() {
            walletTransactionRepository.save(buildTransaction(userId, BigDecimal.valueOf(100), WalletTransaction.TransactionType.CREDIT, "Credit"));
            walletTransactionRepository.save(buildTransaction(userId, BigDecimal.valueOf(30), WalletTransaction.TransactionType.DEBIT, "Debit"));

            Page<WalletTransaction> result = walletTransactionRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

            assertThat(result.getContent()).extracting(WalletTransaction::getType)
                    .containsExactlyInAnyOrder(
                            WalletTransaction.TransactionType.CREDIT,
                            WalletTransaction.TransactionType.DEBIT
                    );
        }
    }
}