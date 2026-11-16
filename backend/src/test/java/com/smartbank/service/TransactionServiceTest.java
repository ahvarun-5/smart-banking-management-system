package com.smartbank.service;

import com.smartbank.dto.DepositRequest;
import com.smartbank.dto.TransactionResponse;
import com.smartbank.dto.TransferRequest;
import com.smartbank.dto.WithdrawRequest;
import com.smartbank.entity.*;
import com.smartbank.exception.AccountInactiveException;
import com.smartbank.exception.InsufficientBalanceException;
import com.smartbank.exception.InvalidTransactionException;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.exception.UnauthorizedAccessException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.TransactionRepository;
import com.smartbank.repository.UserRepository;
import com.smartbank.service.impl.TransactionServiceImpl;
import com.smartbank.util.TransactionReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionReferenceGenerator referenceGenerator;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User senderUser;
    private User receiverUser;
    private Account senderAccount;
    private Account receiverAccount;

    @BeforeEach
    void setUp() {
        senderUser = new User("Sender User", "sender@example.com", "9811111111", "pwd", "Address 1", Role.ROLE_CUSTOMER);
        senderUser.setId(1L);

        receiverUser = new User("Receiver User", "receiver@example.com", "9822222222", "pwd", "Address 2", Role.ROLE_CUSTOMER);
        receiverUser.setId(2L);

        senderAccount = new Account("SB-1000000001", senderUser, AccountType.SAVINGS, new BigDecimal("5000.00"), AccountStatus.ACTIVE);
        senderAccount.setId(101L);

        receiverAccount = new Account("CA-2000000002", receiverUser, AccountType.CURRENT, new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        receiverAccount.setId(102L);
    }

    @Test
    @DisplayName("Deposit - Successful deposit increases balance and creates transaction record")
    void testDepositSuccess() {
        DepositRequest request = new DepositRequest("SB-1000000001", new BigDecimal("1500.00"), "Cash Deposit");
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(accountRepository.findByAccountNumberWithLock("SB-1000000001")).thenReturn(Optional.of(senderAccount));
        when(referenceGenerator.generateReference()).thenReturn("TXN-20261115-DEP01");

        Transaction savedTx = new Transaction(
                "TXN-20261115-DEP01",
                senderAccount,
                null,
                TransactionType.DEPOSIT,
                new BigDecimal("1500.00"),
                TransactionStatus.SUCCESS,
                "Cash Deposit"
        );
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        TransactionResponse response = transactionService.deposit(request, "sender@example.com");

        assertThat(response).isNotNull();
        assertThat(senderAccount.getBalance()).isEqualByComparingTo("6500.00");
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.getAmount()).isEqualByComparingTo("1500.00");

        verify(accountRepository, times(1)).save(senderAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Deposit - Inactive account throws AccountInactiveException")
    void testDepositInactiveAccountThrows() {
        senderAccount.setAccountStatus(AccountStatus.FROZEN);
        DepositRequest request = new DepositRequest("SB-1000000001", new BigDecimal("500.00"), "Deposit");
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(accountRepository.findByAccountNumberWithLock("SB-1000000001")).thenReturn(Optional.of(senderAccount));

        assertThatThrownBy(() -> transactionService.deposit(request, "sender@example.com"))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessageContaining("FROZEN");

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deposit - Invalid negative or zero amount throws InvalidTransactionException")
    void testDepositInvalidAmountThrows() {
        DepositRequest request = new DepositRequest("SB-1000000001", new BigDecimal("-100.00"), "Negative Deposit");

        assertThatThrownBy(() -> transactionService.deposit(request, "sender@example.com"))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("strictly positive");

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deposit - User attempting to deposit into unowned account throws UnauthorizedAccessException")
    void testDepositUnauthorizedThrows() {
        DepositRequest request = new DepositRequest("SB-1000000001", new BigDecimal("500.00"), "Deposit");
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiverUser));
        when(accountRepository.findByAccountNumberWithLock("SB-1000000001")).thenReturn(Optional.of(senderAccount));

        assertThatThrownBy(() -> transactionService.deposit(request, "receiver@example.com"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("access");
    }

    @Test
    @DisplayName("Withdrawal - Successful withdrawal decreases balance")
    void testWithdrawSuccess() {
        WithdrawRequest request = new WithdrawRequest("SB-1000000001", new BigDecimal("2000.00"), "ATM Cash Withdrawal");
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(accountRepository.findByAccountNumberWithLock("SB-1000000001")).thenReturn(Optional.of(senderAccount));
        when(referenceGenerator.generateReference()).thenReturn("TXN-20261115-WTH01");

        Transaction savedTx = new Transaction(
                "TXN-20261115-WTH01",
                senderAccount,
                null,
                TransactionType.WITHDRAWAL,
                new BigDecimal("2000.00"),
                TransactionStatus.SUCCESS,
                "ATM Cash Withdrawal"
        );
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

        TransactionResponse response = transactionService.withdraw(request, "sender@example.com");

        assertThat(response).isNotNull();
        assertThat(senderAccount.getBalance()).isEqualByComparingTo("3000.00");
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(response.getAmount()).isEqualByComparingTo("2000.00");

        verify(accountRepository, times(1)).save(senderAccount);
    }

    @Test
    @DisplayName("Withdrawal - Exceeding available balance throws InsufficientBalanceException")
    void testWithdrawInsufficientBalanceThrows() {
        WithdrawRequest request = new WithdrawRequest("SB-1000000001", new BigDecimal("7000.00"), "Exceeding Balance");
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(accountRepository.findByAccountNumberWithLock("SB-1000000001")).thenReturn(Optional.of(senderAccount));

        assertThatThrownBy(() -> transactionService.withdraw(request, "sender@example.com"))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient funds");

        assertThat(senderAccount.getBalance()).isEqualByComparingTo("5000.00"); // Unaltered
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transfer - Successful fund transfer debits sender, credits receiver and creates records")
    void testTransferSuccess() {
        TransferRequest request = new TransferRequest("SB-1000000001", "CA-2000000002", new BigDecimal("1000.00"), "Rent payment");
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));

        // CA-2000000002 < SB-1000000001 alphabetically, so CA is locked first
        when(accountRepository.findByAccountNumberWithLock("CA-2000000002")).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.findByAccountNumberWithLock("SB-1000000001")).thenReturn(Optional.of(senderAccount));

        when(referenceGenerator.generateReference()).thenReturn("TXN-REF-OUT", "TXN-REF-IN");

        Transaction senderTx = new Transaction(
                "TXN-REF-OUT",
                senderAccount,
                receiverAccount,
                TransactionType.TRANSFER_OUT,
                new BigDecimal("1000.00"),
                TransactionStatus.SUCCESS,
                "Rent payment to account CA-2000000002"
        );
        when(transactionRepository.save(any(Transaction.class))).thenReturn(senderTx);

        TransactionResponse response = transactionService.transfer(request, "sender@example.com");

        assertThat(response).isNotNull();
        assertThat(senderAccount.getBalance()).isEqualByComparingTo("4000.00");
        assertThat(receiverAccount.getBalance()).isEqualByComparingTo("2000.00");

        verify(accountRepository, times(1)).save(senderAccount);
        verify(accountRepository, times(1)).save(receiverAccount);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Transfer - Deterministic lock ordering locks lower account number first to eliminate deadlocks")
    void testTransferDeterministicLockingOrder() {
        TransferRequest request = new TransferRequest("SB-1000000001", "CA-2000000002", new BigDecimal("500.00"), "Transfer");
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));

        // "CA..." < "SB..."
        when(accountRepository.findByAccountNumberWithLock("CA-2000000002")).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.findByAccountNumberWithLock("SB-1000000001")).thenReturn(Optional.of(senderAccount));
        when(referenceGenerator.generateReference()).thenReturn("REF1", "REF2");
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transactionService.transfer(request, "sender@example.com");

        InOrder inOrder = inOrder(accountRepository);
        inOrder.verify(accountRepository).findByAccountNumberWithLock("CA-2000000002");
        inOrder.verify(accountRepository).findByAccountNumberWithLock("SB-1000000001");
    }

    @Test
    @DisplayName("Transfer - Transfer to same account throws InvalidTransactionException")
    void testTransferSameAccountThrows() {
        TransferRequest request = new TransferRequest("SB-1000000001", "SB-1000000001", new BigDecimal("500.00"), "Self Transfer");

        assertThatThrownBy(() -> transactionService.transfer(request, "sender@example.com"))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("cannot be the same");

        verify(accountRepository, never()).findByAccountNumberWithLock(anyString());
    }

    @Test
    @DisplayName("Transfer - Insufficient sender balance throws InsufficientBalanceException")
    void testTransferInsufficientBalanceThrows() {
        TransferRequest request = new TransferRequest("SB-1000000001", "CA-2000000002", new BigDecimal("9999.00"), "Big Transfer");
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(accountRepository.findByAccountNumberWithLock("CA-2000000002")).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.findByAccountNumberWithLock("SB-1000000001")).thenReturn(Optional.of(senderAccount));

        assertThatThrownBy(() -> transactionService.transfer(request, "sender@example.com"))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient funds");

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transfer - Inactive receiver account throws AccountInactiveException")
    void testTransferInactiveReceiverThrows() {
        receiverAccount.setAccountStatus(AccountStatus.INACTIVE);
        TransferRequest request = new TransferRequest("SB-1000000001", "CA-2000000002", new BigDecimal("500.00"), "Transfer");
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(accountRepository.findByAccountNumberWithLock("CA-2000000002")).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.findByAccountNumberWithLock("SB-1000000001")).thenReturn(Optional.of(senderAccount));

        assertThatThrownBy(() -> transactionService.transfer(request, "sender@example.com"))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessageContaining("INACTIVE");

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deactivated Customer - Banking operations are blocked for deactivated users")
    void testDeactivatedCustomerOperationsBlocked() {
        senderUser.setEnabled(false);
        DepositRequest request = new DepositRequest("SB-1000000001", new BigDecimal("500.00"), "Deposit");
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));

        assertThatThrownBy(() -> transactionService.deposit(request, "sender@example.com"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("deactivated");

        verify(accountRepository, never()).findByAccountNumberWithLock(anyString());
    }

    @Test
    @DisplayName("Inquire Transaction - Customer can view their own transaction by reference")
    void testGetTransactionByReferenceSuccess() {
        Transaction tx = new Transaction("TXN-001", senderAccount, null, TransactionType.DEPOSIT, new BigDecimal("500.00"), TransactionStatus.SUCCESS, "Deposit");
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(transactionRepository.findByTransactionReference("TXN-001")).thenReturn(Optional.of(tx));

        TransactionResponse response = transactionService.getTransactionByReference("TXN-001", "sender@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getTransactionReference()).isEqualTo("TXN-001");
    }

    @Test
    @DisplayName("Inquire Transaction - Unauthorized customer viewing another's transaction throws UnauthorizedAccessException")
    void testGetTransactionByReferenceUnauthorizedThrows() {
        Transaction tx = new Transaction("TXN-001", senderAccount, null, TransactionType.DEPOSIT, new BigDecimal("500.00"), TransactionStatus.SUCCESS, "Deposit");
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiverUser));
        when(transactionRepository.findByTransactionReference("TXN-001")).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> transactionService.getTransactionByReference("TXN-001", "receiver@example.com"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("permission");
    }
}
