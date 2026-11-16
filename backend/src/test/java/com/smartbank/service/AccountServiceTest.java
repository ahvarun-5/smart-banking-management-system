package com.smartbank.service;

import com.smartbank.dto.AccountCreateRequest;
import com.smartbank.dto.AccountResponse;
import com.smartbank.entity.*;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.exception.UnauthorizedAccessException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.TransactionRepository;
import com.smartbank.repository.UserRepository;
import com.smartbank.service.impl.AccountServiceImpl;
import com.smartbank.util.AccountNumberGenerator;
import com.smartbank.util.TransactionReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private TransactionReferenceGenerator transactionReferenceGenerator;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User testUser;
    private User otherUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User("Rahul Verma", "rahul@example.com", "9876543210", "hashedPwd", "Mumbai", Role.ROLE_CUSTOMER);
        testUser.setId(1L);

        otherUser = new User("Sneha Patel", "sneha@example.com", "9876543211", "hashedPwd", "Delhi", Role.ROLE_CUSTOMER);
        otherUser.setId(2L);

        adminUser = new User("Admin User", "admin@smartbank.com", "9876543212", "hashedPwd", "HQ", Role.ROLE_ADMIN);
        adminUser.setId(99L);
    }

    @Test
    @DisplayName("Create SAVINGS account with initial deposit - Should save account and create deposit transaction")
    void testCreateAccountWithInitialDeposit() {
        AccountCreateRequest request = new AccountCreateRequest(AccountType.SAVINGS, new BigDecimal("1000.00"));
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(testUser));
        when(accountNumberGenerator.generateAccountNumber(AccountType.SAVINGS)).thenReturn("SB-1001234567");
        when(transactionReferenceGenerator.generateReference()).thenReturn("TXN-20261115-A1B2C3D4");

        Account savedAccount = new Account("SB-1001234567", testUser, AccountType.SAVINGS, new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        savedAccount.setId(10L);
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        AccountResponse response = accountService.createAccount(request, "rahul@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo("SB-1001234567");
        assertThat(response.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(response.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(response.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Create CURRENT account with zero deposit - Should save account without deposit transaction")
    void testCreateAccountWithZeroDeposit() {
        AccountCreateRequest request = new AccountCreateRequest(AccountType.CURRENT, BigDecimal.ZERO);
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(testUser));
        when(accountNumberGenerator.generateAccountNumber(AccountType.CURRENT)).thenReturn("CA-2009876543");

        Account savedAccount = new Account("CA-2009876543", testUser, AccountType.CURRENT, BigDecimal.ZERO, AccountStatus.ACTIVE);
        savedAccount.setId(11L);
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        AccountResponse response = accountService.createAccount(request, "rahul@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo("CA-2009876543");
        assertThat(response.getBalance()).isEqualByComparingTo("0.00");
        assertThat(response.getAccountType()).isEqualTo(AccountType.CURRENT);

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Create account for disabled user - Should throw UnauthorizedAccessException")
    void testCreateAccountDisabledUserThrows() {
        testUser.setEnabled(false);
        AccountCreateRequest request = new AccountCreateRequest(AccountType.SAVINGS, BigDecimal.ZERO);
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> accountService.createAccount(request, "rahul@example.com"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("disabled");

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get accounts by user - Should return user's account list")
    void testGetAccountsByUser() {
        Account acc1 = new Account("SB-1001", testUser, AccountType.SAVINGS, new BigDecimal("500.00"), AccountStatus.ACTIVE);
        acc1.setId(1L);
        Account acc2 = new Account("CA-2002", testUser, AccountType.CURRENT, new BigDecimal("1500.00"), AccountStatus.ACTIVE);
        acc2.setId(2L);

        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(acc1, acc2));

        List<AccountResponse> responses = accountService.getAccountsByUser("rahul@example.com");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getAccountNumber()).isEqualTo("SB-1001");
        assertThat(responses.get(1).getAccountNumber()).isEqualTo("CA-2002");
    }

    @Test
    @DisplayName("Get account by number as owner - Should return account details")
    void testGetAccountByNumberOwnerSuccess() {
        Account account = new Account("SB-1001", testUser, AccountType.SAVINGS, new BigDecimal("500.00"), AccountStatus.ACTIVE);
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByAccountNumber("SB-1001")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountByNumber("SB-1001", "rahul@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo("SB-1001");
    }

    @Test
    @DisplayName("Get account by number as unauthorized customer - Should throw UnauthorizedAccessException")
    void testGetAccountByNumberUnauthorizedCustomerThrows() {
        Account account = new Account("SB-1001", otherUser, AccountType.SAVINGS, new BigDecimal("500.00"), AccountStatus.ACTIVE);
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByAccountNumber("SB-1001")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.getAccountByNumber("SB-1001", "rahul@example.com"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("permission");
    }

    @Test
    @DisplayName("Get account by number as ADMIN - Should allow access to any account")
    void testGetAccountByNumberAdminAllowed() {
        Account account = new Account("SB-1001", testUser, AccountType.SAVINGS, new BigDecimal("500.00"), AccountStatus.ACTIVE);
        when(userRepository.findByEmail("admin@smartbank.com")).thenReturn(Optional.of(adminUser));
        when(accountRepository.findByAccountNumber("SB-1001")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountByNumber("SB-1001", "admin@smartbank.com");

        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo("SB-1001");
    }

    @Test
    @DisplayName("Get account by unknown number - Should throw ResourceNotFoundException")
    void testGetAccountByNumberNotFoundThrows() {
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByAccountNumber("SB-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountByNumber("SB-9999", "rahul@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
