package com.smartbank.dto;

import com.smartbank.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AccountCreateRequest {

    @NotNull(message = "Account type is required (SAVINGS or CURRENT)")
    private AccountType accountType;

    @DecimalMin(value = "0.00", inclusive = true, message = "Initial deposit amount cannot be negative")
    private BigDecimal initialDeposit = BigDecimal.ZERO;

    public AccountCreateRequest() {
    }

    public AccountCreateRequest(AccountType accountType, BigDecimal initialDeposit) {
        this.accountType = accountType;
        this.initialDeposit = (initialDeposit != null) ? initialDeposit : BigDecimal.ZERO;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getInitialDeposit() {
        return initialDeposit;
    }

    public void setInitialDeposit(BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
    }
}
