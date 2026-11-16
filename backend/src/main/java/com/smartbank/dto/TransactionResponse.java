package com.smartbank.dto;

import com.smartbank.entity.Transaction;
import com.smartbank.entity.TransactionStatus;
import com.smartbank.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private String transactionReference;
    private String accountNumber;
    private String relatedAccountNumber;
    private TransactionType transactionType;
    private BigDecimal amount;
    private TransactionStatus transactionStatus;
    private String description;
    private LocalDateTime transactionDate;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, String transactionReference, String accountNumber,
                               String relatedAccountNumber, TransactionType transactionType,
                               BigDecimal amount, TransactionStatus transactionStatus,
                               String description, LocalDateTime transactionDate) {
        this.id = id;
        this.transactionReference = transactionReference;
        this.accountNumber = accountNumber;
        this.relatedAccountNumber = relatedAccountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionStatus = transactionStatus;
        this.description = description;
        this.transactionDate = transactionDate;
    }

    public static TransactionResponse fromEntity(Transaction tx) {
        if (tx == null) {
            return null;
        }
        String accNum = tx.getAccount() != null ? tx.getAccount().getAccountNumber() : null;
        String relAccNum = tx.getRelatedAccount() != null ? tx.getRelatedAccount().getAccountNumber() : null;
        return new TransactionResponse(
                tx.getId(),
                tx.getTransactionReference(),
                accNum,
                relAccNum,
                tx.getTransactionType(),
                tx.getAmount(),
                tx.getTransactionStatus(),
                tx.getDescription(),
                tx.getTransactionDate()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getRelatedAccountNumber() {
        return relatedAccountNumber;
    }

    public void setRelatedAccountNumber(String relatedAccountNumber) {
        this.relatedAccountNumber = relatedAccountNumber;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(TransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
}
