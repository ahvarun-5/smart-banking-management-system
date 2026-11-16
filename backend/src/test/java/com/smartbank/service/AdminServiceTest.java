package com.smartbank.service;

import com.smartbank.dto.*;
import com.smartbank.entity.*;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.TransactionRepository;
import com.smartbank.repository.UserRepository;
import com.smartbank.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User customerUser;
    private User adminUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        customerUser = new User("Rajesh Kumar", "rajesh@example.com", "9876500000", "passHash", "Delhi", Role.ROLE_CUSTOMER);
        customerUser.setId(10L);

        adminUser = new User("Admin", "admin@smartbank.com", "9876599999", "passHash", "HQ", Role.ROLE_ADMIN);
        adminUser.setId(1L);

        testAccount = new Account("SB-1001234567", customerUser, AccountType.SAVINGS, new BigDecimal("15000.00"), AccountStatus.ACTIVE);
        testAccount.setId(100L);
    }

    @Test
    @DisplayName("Dashboard Analytics - Correctly aggregates system metrics and recent transactions")
    void testGetDashboardAnalytics() {
        when(userRepository.countByRole(Role.ROLE_CUSTOMER)).thenReturn(25L);
        when(userRepository.countByRoleAndEnabled(Role.ROLE_CUSTOMER, true)).thenReturn(22L);
        when(accountRepository.count()).thenReturn(30L);
        when(accountRepository.countByAccountStatus(AccountStatus.ACTIVE)).thenReturn(28L);
        when(accountRepository.sumTotalBalance()).thenReturn(new BigDecimal("125000.50"));
        when(transactionRepository.sumTotalDeposits()).thenReturn(new BigDecimal("50000.00"));
        when(transactionRepository.sumTotalWithdrawals()).thenReturn(new BigDecimal("20000.00"));
        when(transactionRepository.sumTotalTransfers()).thenReturn(new BigDecimal("15000.00"));
        when(transactionRepository.count()).thenReturn(150L);

        Transaction recentTx = new Transaction(
                "TXN-20261115-01", testAccount, null, TransactionType.DEPOSIT,
                new BigDecimal("5000.00"), TransactionStatus.SUCCESS, "Salary"
        );
        when(transactionRepository.findTop10ByOrderByTransactionDateDesc()).thenReturn(List.of(recentTx));

        AdminDashboardResponse response = adminService.getDashboardAnalytics();

        assertThat(response).isNotNull();
        assertThat(response.getTotalCustomers()).isEqualTo(25L);
        assertThat(response.getTotalActiveCustomers()).isEqualTo(22L);
        assertThat(response.getTotalAccounts()).isEqualTo(30L);
        assertThat(response.getTotalActiveAccounts()).isEqualTo(28L);
        assertThat(response.getTotalAccountBalances()).isEqualByComparingTo("125000.50");
        assertThat(response.getTotalDepositVolume()).isEqualByComparingTo("50000.00");
        assertThat(response.getTotalWithdrawalVolume()).isEqualByComparingTo("20000.00");
        assertThat(response.getTotalTransferVolume()).isEqualByComparingTo("15000.00");
        assertThat(response.getTotalTransactionCount()).isEqualTo(150L);
        assertThat(response.getRecentTransactions()).hasSize(1);
        assertThat(response.getRecentTransactions().get(0).getTransactionReference()).isEqualTo("TXN-20261115-01");
    }

    @Test
    @DisplayName("Get Customers - Lists all customers without query filter")
    void testGetAllCustomersWithoutQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByRole(Role.ROLE_CUSTOMER, pageable))
                .thenReturn(new PageImpl<>(List.of(customerUser), pageable, 1));

        Page<UserResponse> result = adminService.getAllCustomers(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("rajesh@example.com");
    }

    @Test
    @DisplayName("Get Customers - Searches customers with query filter")
    void testGetAllCustomersWithQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.searchUsersByRole(Role.ROLE_CUSTOMER, "Rajesh", pageable))
                .thenReturn(new PageImpl<>(List.of(customerUser), pageable, 1));

        Page<UserResponse> result = adminService.getAllCustomers("Rajesh", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Rajesh Kumar");
        verify(userRepository, times(1)).searchUsersByRole(Role.ROLE_CUSTOMER, "Rajesh", pageable);
    }

    @Test
    @DisplayName("Customer Details - Returns customer info, bank accounts, and aggregated balance")
    void testGetCustomerByIdSuccess() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));
        when(accountRepository.findByUserId(10L)).thenReturn(List.of(testAccount));
        when(accountRepository.sumBalanceByUserId(10L)).thenReturn(new BigDecimal("15000.00"));

        CustomerDetailResponse response = adminService.getCustomerById(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("rajesh@example.com");
        assertThat(response.getAccounts()).hasSize(1);
        assertThat(response.getAccounts().get(0).getAccountNumber()).isEqualTo("SB-1001234567");
        assertThat(response.getTotalBalance()).isEqualByComparingTo("15000.00");
    }

    @Test
    @DisplayName("Customer Details - Nonexistent ID throws ResourceNotFoundException")
    void testGetCustomerByIdNotFoundThrows() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getCustomerById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    @DisplayName("Customer Status Update - Successfully deactivates a customer account")
    void testUpdateCustomerStatusDeactivate() {
        CustomerStatusUpdateRequest request = new CustomerStatusUpdateRequest(false, "Suspicious activity detected");
        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenReturn(customerUser);

        UserResponse response = adminService.updateCustomerStatus(10L, request);

        assertThat(response).isNotNull();
        assertThat(customerUser.isEnabled()).isFalse();
        verify(userRepository, times(1)).save(customerUser);
    }

    @Test
    @DisplayName("Customer Status Update - Attempting to alter ADMIN user throws IllegalArgumentException")
    void testUpdateCustomerStatusAdminThrows() {
        CustomerStatusUpdateRequest request = new CustomerStatusUpdateRequest(false, "Deactivate Admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> adminService.updateCustomerStatus(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("administrative accounts");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Account Listing - Filters accounts by type, status, and query")
    void testGetAllAccountsFiltered() {
        Pageable pageable = PageRequest.of(0, 10);
        when(accountRepository.filterAdminAccounts(AccountType.SAVINGS, AccountStatus.ACTIVE, "SB-100", pageable))
                .thenReturn(new PageImpl<>(List.of(testAccount), pageable, 1));

        Page<AccountResponse> result = adminService.getAllAccounts(AccountType.SAVINGS, AccountStatus.ACTIVE, "SB-100", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAccountNumber()).isEqualTo("SB-1001234567");
    }

    @Test
    @DisplayName("Account Status Update - Freezes an account successfully")
    void testUpdateAccountStatusFreeze() {
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest(AccountStatus.FROZEN, "Court order / compliance hold");
        when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountResponse response = adminService.updateAccountStatus(100L, request);

        assertThat(response).isNotNull();
        assertThat(testAccount.getAccountStatus()).isEqualTo(AccountStatus.FROZEN);
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    @DisplayName("Transaction Monitoring - Retrieves system-wide transactions with multi-criteria filters")
    void testGetAllTransactionsFiltered() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction tx = new Transaction(
                "TXN-20261115-01", testAccount, null, TransactionType.DEPOSIT,
                new BigDecimal("5000.00"), TransactionStatus.SUCCESS, "Deposit"
        );
        when(transactionRepository.filterAdminTransactions(
                eq(TransactionType.DEPOSIT), eq(TransactionStatus.SUCCESS), eq("SB-1001234567"),
                eq("Rajesh"), any(), any(), eq("TXN-"), eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(tx), pageable, 1));

        Page<TransactionResponse> result = adminService.getAllTransactions(
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS, "SB-1001234567",
                "Rajesh", null, null, "TXN-", pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTransactionReference()).isEqualTo("TXN-20261115-01");
    }
}
