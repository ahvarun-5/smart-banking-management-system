package com.smartbank.service.impl;

import com.smartbank.dto.*;
import com.smartbank.entity.*;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.TransactionRepository;
import com.smartbank.repository.UserRepository;
import com.smartbank.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AdminServiceImpl(UserRepository userRepository,
                            AccountRepository accountRepository,
                            TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardAnalytics() {
        log.info("Calculating system-wide administrative dashboard analytics");

        long totalCustomers = userRepository.countByRole(Role.ROLE_CUSTOMER);
        long totalActiveCustomers = userRepository.countByRoleAndEnabled(Role.ROLE_CUSTOMER, true);

        long totalAccounts = accountRepository.count();
        long totalActiveAccounts = accountRepository.countByAccountStatus(AccountStatus.ACTIVE);

        BigDecimal totalBalances = accountRepository.sumTotalBalance();
        BigDecimal totalDeposits = transactionRepository.sumTotalDeposits();
        BigDecimal totalWithdrawals = transactionRepository.sumTotalWithdrawals();
        BigDecimal totalTransfers = transactionRepository.sumTotalTransfers();
        long totalTransactions = transactionRepository.count();

        List<TransactionResponse> recentTransactions = transactionRepository.findTop10ByOrderByTransactionDateDesc()
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());

        return new AdminDashboardResponse(
                totalCustomers,
                totalActiveCustomers,
                totalAccounts,
                totalActiveAccounts,
                totalBalances,
                totalDeposits,
                totalWithdrawals,
                totalTransfers,
                totalTransactions,
                recentTransactions
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllCustomers(String query, Pageable pageable) {
        log.debug("Fetching customer list with query filter: {}", query);

        if (query != null && !query.trim().isEmpty()) {
            return userRepository.searchUsersByRole(Role.ROLE_CUSTOMER, query.trim(), pageable)
                    .map(UserResponse::fromEntity);
        }
        return userRepository.findByRole(Role.ROLE_CUSTOMER, pageable)
                .map(UserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerById(Long customerId) {
        log.debug("Fetching customer detail for id: {}", customerId);

        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        List<AccountResponse> accounts = accountRepository.findByUserId(customerId)
                .stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());

        BigDecimal totalBalance = accountRepository.sumBalanceByUserId(customerId);

        return CustomerDetailResponse.fromEntity(user, accounts, totalBalance);
    }

    @Override
    @Transactional
    public UserResponse updateCustomerStatus(Long customerId, CustomerStatusUpdateRequest request) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot modify administrative accounts through customer status endpoint");
        }

        user.setEnabled(request.getEnabled());
        User updated = userRepository.save(user);

        log.info("Audit: Administrator changed status of customer [id={}, email={}] to enabled={}. Reason: {}",
                user.getId(), user.getEmail(), request.getEnabled(), request.getReason());

        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccounts(AccountType type, AccountStatus status, String query, Pageable pageable) {
        log.debug("Filtering accounts by type={}, status={}, query={}", type, status, query);

        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return accountRepository.filterAdminAccounts(type, status, cleanQuery, pageable)
                .map(AccountResponse::fromEntity);
    }

    @Override
    @Transactional
    public AccountResponse updateAccountStatus(Long accountId, AccountStatusUpdateRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        AccountStatus oldStatus = account.getAccountStatus();
        account.setAccountStatus(request.getAccountStatus());
        Account savedAccount = accountRepository.save(account);

        log.info("Audit: Administrator changed account status [id={}, accountNumber={}] from {} to {}. Reason: {}",
                account.getId(), account.getAccountNumber(), oldStatus, request.getAccountStatus(), request.getReason());

        return AccountResponse.fromEntity(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(
            TransactionType type,
            TransactionStatus status,
            String accountNumber,
            String customerQuery,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String reference,
            Pageable pageable) {

        log.debug("Admin filtering transactions by type={}, status={}, account={}, customer={}",
                type, status, accountNumber, customerQuery);

        String cleanAccount = (accountNumber != null && !accountNumber.trim().isEmpty()) ? accountNumber.trim() : null;
        String cleanCustomer = (customerQuery != null && !customerQuery.trim().isEmpty()) ? customerQuery.trim() : null;
        String cleanRef = (reference != null && !reference.trim().isEmpty()) ? reference.trim() : null;

        return transactionRepository.filterAdminTransactions(
                type,
                status,
                cleanAccount,
                cleanCustomer,
                startDate,
                endDate,
                cleanRef,
                pageable
        ).map(TransactionResponse::fromEntity);
    }
}
