package com.smartbank.dto;

import com.smartbank.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class AccountStatusUpdateRequest {

    @NotNull(message = "Account status is required (ACTIVE, INACTIVE, FROZEN)")
    private AccountStatus accountStatus;

    private String reason;

    public AccountStatusUpdateRequest() {
    }

    public AccountStatusUpdateRequest(AccountStatus accountStatus, String reason) {
        this.accountStatus = accountStatus;
        this.reason = reason;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
