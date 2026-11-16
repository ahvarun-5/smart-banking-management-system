package com.smartbank.service;

import com.smartbank.dto.*;
import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.AccountType;
import com.smartbank.entity.TransactionStatus;
import com.smartbank.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AdminService {

    AdminDashboardResponse getDashboardAnalytics();

    Page<UserResponse> getAllCustomers(String query, Pageable pageable);

    CustomerDetailResponse getCustomerById(Long customerId);

    UserResponse updateCustomerStatus(Long customerId, CustomerStatusUpdateRequest request);

    Page<AccountResponse> getAllAccounts(AccountType type, AccountStatus status, String query, Pageable pageable);

    AccountResponse updateAccountStatus(Long accountId, AccountStatusUpdateRequest request);

    Page<TransactionResponse> getAllTransactions(
            TransactionType type,
            TransactionStatus status,
            String accountNumber,
            String customerQuery,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String reference,
            Pageable pageable
    );
}
