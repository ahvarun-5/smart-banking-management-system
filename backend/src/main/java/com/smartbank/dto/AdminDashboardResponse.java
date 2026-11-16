package com.smartbank.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class AdminDashboardResponse {

    private long totalCustomers;
    private long totalActiveCustomers;
    private long totalAccounts;
    private long totalActiveAccounts;
    private BigDecimal totalAccountBalances;
    private BigDecimal totalDepositVolume;
    private BigDecimal totalWithdrawalVolume;
    private BigDecimal totalTransferVolume;
    private long totalTransactionCount;
    private List<TransactionResponse> recentTransactions;

    public AdminDashboardResponse() {
    }

    public AdminDashboardResponse(long totalCustomers, long totalActiveCustomers,
                                  long totalAccounts, long totalActiveAccounts,
                                  BigDecimal totalAccountBalances, BigDecimal totalDepositVolume,
                                  BigDecimal totalWithdrawalVolume, BigDecimal totalTransferVolume,
                                  long totalTransactionCount, List<TransactionResponse> recentTransactions) {
        this.totalCustomers = totalCustomers;
        this.totalActiveCustomers = totalActiveCustomers;
        this.totalAccounts = totalAccounts;
        this.totalActiveAccounts = totalActiveAccounts;
        this.totalAccountBalances = (totalAccountBalances != null) ? totalAccountBalances : BigDecimal.ZERO;
        this.totalDepositVolume = (totalDepositVolume != null) ? totalDepositVolume : BigDecimal.ZERO;
        this.totalWithdrawalVolume = (totalWithdrawalVolume != null) ? totalWithdrawalVolume : BigDecimal.ZERO;
        this.totalTransferVolume = (totalTransferVolume != null) ? totalTransferVolume : BigDecimal.ZERO;
        this.totalTransactionCount = totalTransactionCount;
        this.recentTransactions = (recentTransactions != null) ? recentTransactions : Collections.emptyList();
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalActiveCustomers() {
        return totalActiveCustomers;
    }

    public void setTotalActiveCustomers(long totalActiveCustomers) {
        this.totalActiveCustomers = totalActiveCustomers;
    }

    public long getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(long totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public long getTotalActiveAccounts() {
        return totalActiveAccounts;
    }

    public void setTotalActiveAccounts(long totalActiveAccounts) {
        this.totalActiveAccounts = totalActiveAccounts;
    }

    public BigDecimal getTotalAccountBalances() {
        return totalAccountBalances;
    }

    public void setTotalAccountBalances(BigDecimal totalAccountBalances) {
        this.totalAccountBalances = totalAccountBalances;
    }

    public BigDecimal getTotalDepositVolume() {
        return totalDepositVolume;
    }

    public void setTotalDepositVolume(BigDecimal totalDepositVolume) {
        this.totalDepositVolume = totalDepositVolume;
    }

    public BigDecimal getTotalWithdrawalVolume() {
        return totalWithdrawalVolume;
    }

    public void setTotalWithdrawalVolume(BigDecimal totalWithdrawalVolume) {
        this.totalWithdrawalVolume = totalWithdrawalVolume;
    }

    public BigDecimal getTotalTransferVolume() {
        return totalTransferVolume;
    }

    public void setTotalTransferVolume(BigDecimal totalTransferVolume) {
        this.totalTransferVolume = totalTransferVolume;
    }

    public long getTotalTransactionCount() {
        return totalTransactionCount;
    }

    public void setTotalTransactionCount(long totalTransactionCount) {
        this.totalTransactionCount = totalTransactionCount;
    }

    public List<TransactionResponse> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<TransactionResponse> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }
}
