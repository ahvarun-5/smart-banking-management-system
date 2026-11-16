package com.smartbank.controller;

import com.smartbank.dto.DepositRequest;
import com.smartbank.dto.TransactionResponse;
import com.smartbank.dto.TransferRequest;
import com.smartbank.dto.WithdrawRequest;
import com.smartbank.entity.TransactionType;
import com.smartbank.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request,
                                                       Authentication authentication) {
        TransactionResponse response = transactionService.deposit(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest request,
                                                        Authentication authentication) {
        TransactionResponse response = transactionService.withdraw(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                        Authentication authentication) {
        TransactionResponse response = transactionService.transfer(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getCustomerTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String reference,
            @PageableDefault(size = 10, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        Page<TransactionResponse> transactions = transactionService.getCustomerTransactions(
                authentication.getName(),
                type,
                startDate,
                endDate,
                reference,
                pageable
        );
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Page<TransactionResponse>> getAccountTransactions(
            @PathVariable String accountNumber,
            @PageableDefault(size = 10, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        Page<TransactionResponse> transactions = transactionService.getAccountTransactions(
                accountNumber,
                authentication.getName(),
                pageable
        );
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{reference}")
    public ResponseEntity<TransactionResponse> getTransactionByReference(
            @PathVariable String reference,
            Authentication authentication) {

        TransactionResponse response = transactionService.getTransactionByReference(reference, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
