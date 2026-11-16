package com.smartbank.dto;

import com.smartbank.entity.Account;
import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {

    private Long id;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus accountStatus;
    private LocalDateTime createdAt;
    private String ownerName;
    private String ownerEmail;

    public AccountResponse() {
    }

    public AccountResponse(Long id, String accountNumber, AccountType accountType, BigDecimal balance,
                           AccountStatus accountStatus, LocalDateTime createdAt, String ownerName, String ownerEmail) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
    }

    public static AccountResponse fromEntity(Account account) {
        if (account == null) {
            return null;
        }
        String ownerName = account.getUser() != null ? account.getUser().getFullName() : null;
        String ownerEmail = account.getUser() != null ? account.getUser().getEmail() : null;
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getAccountStatus(),
                account.getCreatedAt(),
                ownerName,
                ownerEmail
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }
}
