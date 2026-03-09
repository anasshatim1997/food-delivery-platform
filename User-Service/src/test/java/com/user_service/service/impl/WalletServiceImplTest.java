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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private UUID userId;
    private User user;
    private Customer customer;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);

        customer = new Customer();
        customer.setId(userId);
        customer.setWalletBalance(BigDecimal.valueOf(100));
    }

    @Nested
    class GetBalance {

        @Test
        void returnsBalanceWhenUserAndCustomerExist() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));

            WalletBalanceResponse result = walletService.getBalance(userId);

            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.getCurrency()).isEqualTo("MAD");
        }

        @Test
        void throwsWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.getBalance(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        void throwsWhenCustomerNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.getBalance(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer profile not found");
        }
    }

    @Nested
    class GetTransactions {

        @Test
        void returnsPagedTransactionsForUser() {
            WalletTransaction tx = WalletTransaction.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .amount(BigDecimal.valueOf(50))
                    .type(WalletTransaction.TransactionType.CREDIT)
                    .description("Wallet top-up")
                    .referenceType("PAYMENT")
                    .referenceId(null)
                    .build();

            Pageable pageable = PageRequest.of(0, 10);
            Page<WalletTransaction> page = new PageImpl<>(List.of(tx));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);

            Page<TransactionResponse> result = walletService.getTransactions(userId, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(result.getContent().get(0).getType()).isEqualTo(WalletTransaction.TransactionType.CREDIT);
        }

        @Test
        void throwsWhenUserNotFoundOnGetTransactions() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.getTransactions(userId, PageRequest.of(0, 10)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    class TopUp {

        @Test
        void creditsBalanceAndSavesTransactionAndReturnsNewBalance() {
            TopUpRequest request = new TopUpRequest();
            request.setAmount(BigDecimal.valueOf(50));
            request.setPaymentMethodId("pm_test_123");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any())).thenReturn(customer);
            when(walletTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WalletBalanceResponse result = walletService.topUp(userId, request);

            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(150));
            assertThat(result.getCurrency()).isEqualTo("MAD");

            ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
            verify(walletTransactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getType()).isEqualTo(WalletTransaction.TransactionType.CREDIT);
            assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(txCaptor.getValue().getDescription()).isEqualTo("Wallet top-up");
        }

        @Test
        void throwsWhenCustomerNotFoundOnTopUp() {
            TopUpRequest request = new TopUpRequest();
            request.setAmount(BigDecimal.valueOf(50));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.topUp(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer profile not found");
        }
    }

    @Nested
    class Debit {

        @Test
        void subtractsAmountAndSavesDebitTransaction() {
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any())).thenReturn(customer);
            when(walletTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            walletService.debit(userId, BigDecimal.valueOf(40), "Order payment", "ORDER", UUID.randomUUID());

            assertThat(customer.getWalletBalance()).isEqualByComparingTo(BigDecimal.valueOf(60));

            ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
            verify(walletTransactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getType()).isEqualTo(WalletTransaction.TransactionType.DEBIT);
            assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(40));
        }

        @Test
        void throwsInsufficientBalanceWhenAmountExceedsBalance() {
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> walletService.debit(userId, BigDecimal.valueOf(200), "Order", "ORDER", null))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Insufficient wallet balance");

            verify(walletTransactionRepository, never()).save(any());
        }

        @Test
        void throwsWhenCustomerNotFoundOnDebit() {
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.debit(userId, BigDecimal.valueOf(10), "desc", "ORDER", null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer profile not found");
        }
    }

    @Nested
    class Credit {

        @Test
        void addsAmountAndSavesCreditTransaction() {
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any())).thenReturn(customer);
            when(walletTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            walletService.credit(userId, BigDecimal.valueOf(25), "Refund", "REFUND", null);

            assertThat(customer.getWalletBalance()).isEqualByComparingTo(BigDecimal.valueOf(125));

            ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
            verify(walletTransactionRepository).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getType()).isEqualTo(WalletTransaction.TransactionType.CREDIT);
            assertThat(txCaptor.getValue().getDescription()).isEqualTo("Refund");
        }

        @Test
        void throwsWhenCustomerNotFoundOnCredit() {
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.credit(userId, BigDecimal.valueOf(10), "desc", "REFUND", null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Customer profile not found");
        }
    }
}