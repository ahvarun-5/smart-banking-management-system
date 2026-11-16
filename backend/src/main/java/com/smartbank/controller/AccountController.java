package com.smartbank.controller;

import com.smartbank.dto.AccountCreateRequest;
import com.smartbank.dto.AccountResponse;
import com.smartbank.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountCreateRequest request,
                                                         Authentication authentication) {
        AccountResponse response = accountService.createAccount(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication) {
        List<AccountResponse> accounts = accountService.getAccountsByUser(authentication.getName());
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id,
                                                          Authentication authentication) {
        AccountResponse response = accountService.getAccountById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber,
                                                              Authentication authentication) {
        AccountResponse response = accountService.getAccountByNumber(accountNumber, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
