package com.smartbank.repository;

import com.smartbank.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class TransactionRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account senderAccount;
    private Account receiverAccount;

    @BeforeEach
    void setUp() {
        User senderUser = new User("Alice Brown", "alice@example.com", "9800000001", "password", "Street 1", Role.ROLE_CUSTOMER);
        User receiverUser = new User("Bob Green", "bob@example.com", "9800000002", "password", "Street 2", Role.ROLE_CUSTOMER);

        userRepository.save(senderUser);
        userRepository.save(receiverUser);

        senderAccount = new Account("SB-990001", senderUser, AccountType.SAVINGS, new BigDecimal("10000.00"), AccountStatus.ACTIVE);
        receiverAccount = new Account("SB-990002", receiverUser, AccountType.SAVINGS, new BigDecimal("5000.00"), AccountStatus.ACTIVE);

        accountRepository.save(senderAccount);
        accountRepository.save(receiverAccount);
    }

    @Test
    @DisplayName("Should save deposit transaction and retrieve by reference")
    void testSaveDepositTransaction() {
        Transaction deposit = new Transaction(
                "TXN-DEP-1001",
                senderAccount,
                null,
                TransactionType.DEPOSIT,
                new BigDecimal("2000.00"),
                TransactionStatus.SUCCESS,
                "ATM cash deposit"
        );
        Transaction saved = transactionRepository.save(deposit);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTransactionDate()).isNotNull();

        Optional<Transaction> found = transactionRepository.findByTransactionReference("TXN-DEP-1001");
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("2000.00");
        assertThat(found.get().getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    @DisplayName("Should save transfer transaction with related account")
    void testSaveTransferTransaction() {
        Transaction transferOut = new Transaction(
                "TXN-TRF-1002",
                senderAccount,
                receiverAccount,
                TransactionType.TRANSFER_OUT,
                new BigDecimal("1500.00"),
                TransactionStatus.SUCCESS,
                "Transfer to Bob"
        );
        transactionRepository.save(transferOut);

        Optional<Transaction> found = transactionRepository.findByTransactionReference("TXN-TRF-1002");
        assertThat(found).isPresent();
        assertThat(found.get().getRelatedAccount()).isNotNull();
        assertThat(found.get().getRelatedAccount().getAccountNumber()).isEqualTo("SB-990002");
    }

    @Test
    @DisplayName("Should filter transactions for customer by account IDs")
    void testFilterCustomerTransactions() {
        Transaction t1 = new Transaction("TXN-1", senderAccount, null, TransactionType.DEPOSIT, new BigDecimal("1000.00"), TransactionStatus.SUCCESS, "Dep 1");
        Transaction t2 = new Transaction("TXN-2", senderAccount, null, TransactionType.WITHDRAWAL, new BigDecimal("500.00"), TransactionStatus.SUCCESS, "Wd 1");
        Transaction t3 = new Transaction("TXN-3", receiverAccount, null, TransactionType.DEPOSIT, new BigDecimal("2000.00"), TransactionStatus.SUCCESS, "Dep 2");

        transactionRepository.save(t1);
        transactionRepository.save(t2);
        transactionRepository.save(t3);

        Page<Transaction> senderOnly = transactionRepository.filterCustomerTransactions(
                List.of(senderAccount.getId()),
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(senderOnly.getContent()).hasSize(2);

        Page<Transaction> senderDepositOnly = transactionRepository.filterCustomerTransactions(
                List.of(senderAccount.getId()),
                TransactionType.DEPOSIT,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(senderDepositOnly.getContent()).hasSize(1);
        assertThat(senderDepositOnly.getContent().get(0).getTransactionReference()).isEqualTo("TXN-1");
    }

    @Test
    @DisplayName("Should calculate total deposits and withdrawals accurately")
    void testAggregateSums() {
        Transaction t1 = new Transaction("TXN-D1", senderAccount, null, TransactionType.DEPOSIT, new BigDecimal("3000.00"), TransactionStatus.SUCCESS, "D1");
        Transaction t2 = new Transaction("TXN-D2", receiverAccount, null, TransactionType.DEPOSIT, new BigDecimal("2000.00"), TransactionStatus.SUCCESS, "D2");
        Transaction t3 = new Transaction("TXN-W1", senderAccount, null, TransactionType.WITHDRAWAL, new BigDecimal("1000.00"), TransactionStatus.SUCCESS, "W1");

        transactionRepository.save(t1);
        transactionRepository.save(t2);
        transactionRepository.save(t3);

        BigDecimal totalDeposits = transactionRepository.sumTotalDeposits();
        BigDecimal totalWithdrawals = transactionRepository.sumTotalWithdrawals();

        assertThat(totalDeposits).isEqualByComparingTo("5000.00");
        assertThat(totalWithdrawals).isEqualByComparingTo("1000.00");

        Transaction transferOut = new Transaction("TXN-T1", senderAccount, receiverAccount, TransactionType.TRANSFER_OUT, new BigDecimal("750.00"), TransactionStatus.SUCCESS, "T1");
        Transaction transferIn = new Transaction("TXN-T2", receiverAccount, senderAccount, TransactionType.TRANSFER_IN, new BigDecimal("750.00"), TransactionStatus.SUCCESS, "T2");
        transactionRepository.save(transferOut);
        transactionRepository.save(transferIn);

        BigDecimal totalTransfers = transactionRepository.sumTotalTransfers();
        // Only TRANSFER_OUT is summed to avoid double-counting
        assertThat(totalTransfers).isEqualByComparingTo("750.00");

        Page<Transaction> adminFiltered = transactionRepository.filterAdminTransactions(
                TransactionType.TRANSFER_OUT, TransactionStatus.SUCCESS, senderAccount.getAccountNumber(),
                null, null, null, null, PageRequest.of(0, 10)
        );
        assertThat(adminFiltered.getContent()).hasSize(1);
        assertThat(adminFiltered.getContent().get(0).getTransactionReference()).isEqualTo("TXN-T1");
    }
}
