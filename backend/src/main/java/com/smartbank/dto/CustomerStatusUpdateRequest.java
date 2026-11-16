package com.smartbank.dto;

import jakarta.validation.constraints.NotNull;

public class CustomerStatusUpdateRequest {

    @NotNull(message = "Enabled status flag is required")
    private Boolean enabled;

    private String reason;

    public CustomerStatusUpdateRequest() {
    }

    public CustomerStatusUpdateRequest(Boolean enabled, String reason) {
        this.enabled = enabled;
        this.reason = reason;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
