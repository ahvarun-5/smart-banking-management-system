package com.smartbank.service;

import com.smartbank.dto.DepositRequest;
import com.smartbank.dto.TransactionResponse;
import com.smartbank.dto.TransferRequest;
import com.smartbank.dto.WithdrawRequest;
import com.smartbank.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface TransactionService {

    TransactionResponse deposit(DepositRequest request, String userEmail);

    TransactionResponse withdraw(WithdrawRequest request, String userEmail);

    TransactionResponse transfer(TransferRequest request, String userEmail);

    Page<TransactionResponse> getCustomerTransactions(
            String userEmail,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String reference,
            Pageable pageable
    );

    Page<TransactionResponse> getAccountTransactions(
            String accountNumber,
            String userEmail,
            Pageable pageable
    );

    TransactionResponse getTransactionByReference(String reference, String userEmail);
}
