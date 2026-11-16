package com.smartbank.service;

import com.smartbank.dto.TransferRequest;
import com.smartbank.dto.WithdrawRequest;
import com.smartbank.entity.*;
import com.smartbank.exception.InsufficientBalanceException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.TransactionRepository;
import com.smartbank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class TransactionConcurrencyAndRollbackTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User userA;
    private User userB;
    private Account accountA;
    private Account accountB;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        userA = new User("User Alpha", "alpha@smartbank.com", "9111111111", "passHash", "City A", Role.ROLE_CUSTOMER);
        userB = new User("User Beta", "beta@smartbank.com", "9222222222", "passHash", "City B", Role.ROLE_CUSTOMER);

        userA = userRepository.save(userA);
        userB = userRepository.save(userB);

        accountA = new Account("SB-1000000001", userA, AccountType.SAVINGS, new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        accountB = new Account("CA-2000000002", userB, AccountType.CURRENT, new BigDecimal("1000.00"), AccountStatus.ACTIVE);

        accountA = accountRepository.save(accountA);
        accountB = accountRepository.save(accountB);
    }

    @Test
    @DisplayName("Rollback Verification - Failed transfer rolls back all changes atomically without partial commits")
    void testFailedTransferRollback() {
        // Attempt transfer exceeding balance
        TransferRequest request = new TransferRequest("SB-1000000001", "CA-2000000002", new BigDecimal("5000.00"), "Excess transfer");

        assertThatThrownBy(() -> transactionService.transfer(request, "alpha@smartbank.com"))
                .isInstanceOf(InsufficientBalanceException.class);

        // Verify balances are completely untouched
        Account refreshedA = accountRepository.findByAccountNumber("SB-1000000001").orElseThrow();
        Account refreshedB = accountRepository.findByAccountNumber("CA-2000000002").orElseThrow();

        assertThat(refreshedA.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(refreshedB.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(transactionRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Concurrency & Deadlock Prevention - Bi-directional simultaneous transfers execute safely with exact total conservation")
    void testConcurrentTransfersSafety() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 5 threads transfer 50 from A to B
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    transactionService.transfer(
                            new TransferRequest("SB-1000000001", "CA-2000000002", new BigDecimal("50.00"), "A to B"),
                            "alpha@smartbank.com"
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 5 threads transfer 50 from B to A simultaneously (potential deadlock scenario if unmanaged)
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    transactionService.transfer(
                            new TransferRequest("CA-2000000002", "SB-1000000001", new BigDecimal("50.00"), "B to A"),
                            "beta@smartbank.com"
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        Account refreshedA = accountRepository.findByAccountNumber("SB-1000000001").orElseThrow();
        Account refreshedB = accountRepository.findByAccountNumber("CA-2000000002").orElseThrow();

        // Total sum across both accounts MUST remain exactly 2000.00 (conservation of funds)
        BigDecimal totalBalance = refreshedA.getBalance().add(refreshedB.getBalance());
        assertThat(totalBalance).isEqualByComparingTo("2000.00");
        assertThat(successCount.get()).isEqualTo(10);
    }

    @Test
    @DisplayName("Concurrent Withdrawals - Multiple simultaneous withdrawals prevent negative balances and double-spend")
    void testConcurrentWithdrawalPreventsNegativeBalance() throws InterruptedException {
        // Account has only 100.00
        accountA.setBalance(new BigDecimal("100.00"));
        accountRepository.save(accountA);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    transactionService.withdraw(
                            new WithdrawRequest("SB-1000000001", new BigDecimal("100.00"), "Concurrent withdrawal"),
                            "alpha@smartbank.com"
                    );
                    successCount.incrementAndGet();
                } catch (InsufficientBalanceException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        Account refreshedA = accountRepository.findByAccountNumber("SB-1000000001").orElseThrow();
        assertThat(refreshedA.getBalance()).isEqualByComparingTo("0.00");
    }
}
