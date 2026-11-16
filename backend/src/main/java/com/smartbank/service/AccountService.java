package com.smartbank.service;

import com.smartbank.dto.AccountCreateRequest;
import com.smartbank.dto.AccountResponse;

import java.util.List;

public interface AccountService {

    AccountResponse createAccount(AccountCreateRequest request, String userEmail);

    List<AccountResponse> getAccountsByUser(String userEmail);

    AccountResponse getAccountByNumber(String accountNumber, String userEmail);

    AccountResponse getAccountById(Long id, String userEmail);
}
