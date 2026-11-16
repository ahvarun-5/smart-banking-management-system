package com.smartbank.controller;

import com.smartbank.dto.*;
import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.AccountType;
import com.smartbank.entity.TransactionStatus;
import com.smartbank.entity.TransactionType;
import com.smartbank.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, String>>> adminPing() {
        return ResponseEntity.ok(ApiResponse.success("Admin authorized access verified", Map.of("status", "UP", "role", "ROLE_ADMIN")));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        AdminDashboardResponse response = adminService.getDashboardAnalytics();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customers")
    public ResponseEntity<Page<UserResponse>> getCustomers(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserResponse> customers = adminService.getAllCustomers(query, pageable);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerDetailResponse> getCustomerById(@PathVariable Long id) {
        CustomerDetailResponse customer = adminService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    @PatchMapping("/customers/{id}/status")
    public ResponseEntity<UserResponse> updateCustomerStatus(
            @PathVariable Long id,
            @Valid @RequestBody CustomerStatusUpdateRequest request) {
        UserResponse response = adminService.updateCustomerStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts")
    public ResponseEntity<Page<AccountResponse>> getAccounts(
            @RequestParam(required = false) AccountType type,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AccountResponse> accounts = adminService.getAllAccounts(type, status, query, pageable);
        return ResponseEntity.ok(accounts);
    }

    @PatchMapping("/accounts/{id}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(
            @PathVariable Long id,
            @Valid @RequestBody AccountStatusUpdateRequest request) {
        AccountResponse response = adminService.updateAccountStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String customerQuery,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String reference,
            @PageableDefault(size = 10, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TransactionResponse> transactions = adminService.getAllTransactions(
                type,
                status,
                accountNumber,
                customerQuery,
                startDate,
                endDate,
                reference,
                pageable
        );
        return ResponseEntity.ok(transactions);
    }
}
