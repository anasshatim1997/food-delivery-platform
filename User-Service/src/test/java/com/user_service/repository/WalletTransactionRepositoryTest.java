package com.user_service.repository;

import com.user_service.entity.WalletTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletTransactionRepositoryTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    private UUID userId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        pageable = PageRequest.of(0, 10);
    }

    @Nested
    class FindByUserIdOrderByCreatedAtDesc {

        @Test
        void returnsTransactionsForCorrectUser() {
            WalletTransaction tx = new WalletTransaction();
            Page<WalletTransaction> page = new PageImpl<>(List.of(tx));
            when(walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);

            Page<WalletTransaction> result = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(walletTransactionRepository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        @Test
        void returnsEmptyPage_whenNoTransactionsExist() {
            Page<WalletTransaction> emptyPage = new PageImpl<>(List.of());
            when(walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(emptyPage);

            Page<WalletTransaction> result = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        void returnsTransactionsInDescendingOrderByCreatedAt() {
            WalletTransaction tx1 = new WalletTransaction();
            WalletTransaction tx2 = new WalletTransaction();
            WalletTransaction tx3 = new WalletTransaction();
            Page<WalletTransaction> page = new PageImpl<>(List.of(tx1, tx2, tx3));
            when(walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);

            Page<WalletTransaction> result = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0)).isEqualTo(tx1);
            assertThat(result.getContent().get(1)).isEqualTo(tx2);
            assertThat(result.getContent().get(2)).isEqualTo(tx3);
        }

        @Test
        void doesNotReturnTransactionsForOtherUser() {
            UUID otherUserId = UUID.randomUUID();
            Page<WalletTransaction> emptyPage = new PageImpl<>(List.of());
            when(walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(otherUserId, pageable)).thenReturn(emptyPage);

            Page<WalletTransaction> result = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(otherUserId, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(walletTransactionRepository, never()).findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        @Test
        void respectsPageableSize() {
            Pageable smallPage = PageRequest.of(0, 2);
            WalletTransaction tx1 = new WalletTransaction();
            WalletTransaction tx2 = new WalletTransaction();
            Page<WalletTransaction> page = new PageImpl<>(List.of(tx1, tx2), smallPage, 5);
            when(walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, smallPage)).thenReturn(page);

            Page<WalletTransaction> result = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, smallPage);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }

        @Test
        void respectsPageableOffset() {
            Pageable secondPage = PageRequest.of(1, 2);
            WalletTransaction tx = new WalletTransaction();
            Page<WalletTransaction> page = new PageImpl<>(List.of(tx), secondPage, 3);
            when(walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, secondPage)).thenReturn(page);

            Page<WalletTransaction> result = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, secondPage);

            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
        }
    }
}