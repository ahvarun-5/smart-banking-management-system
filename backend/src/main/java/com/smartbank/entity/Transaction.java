package com.smartbank.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_transactions_reference", columnList = "transaction_reference", unique = true),
        @Index(name = "idx_transactions_account_id", columnList = "account_id"),
        @Index(name = "idx_transactions_date", columnList = "transaction_date"),
        @Index(name = "idx_transactions_type", columnList = "transaction_type")
    }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Transaction reference is required")
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 36)
    private String transactionReference;

    @NotNull(message = "Primary account is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transactions_account"))
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_account_id", foreignKey = @ForeignKey(name = "fk_transactions_related_account"))
    private Account relatedAccount;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Transaction status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false, length = 20)
    private TransactionStatus transactionStatus = TransactionStatus.SUCCESS;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "transaction_date", nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    public Transaction() {
    }

    public Transaction(String transactionReference, Account account, Account relatedAccount,
                       TransactionType transactionType, BigDecimal amount,
                       TransactionStatus transactionStatus, String description) {
        this.transactionReference = transactionReference;
        this.account = account;
        this.relatedAccount = relatedAccount;
        this.transactionType = transactionType;
        this.amount = (amount != null) ? amount.setScale(2) : BigDecimal.ZERO.setScale(2);
        this.transactionStatus = (transactionStatus != null) ? transactionStatus : TransactionStatus.SUCCESS;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        if (this.transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
        if (this.transactionStatus == null) {
            this.transactionStatus = TransactionStatus.SUCCESS;
        }
        if (this.amount != null) {
            this.amount = this.amount.setScale(2);
        }
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

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Account getRelatedAccount() {
        return relatedAccount;
    }

    public void setRelatedAccount(Account relatedAccount) {
        this.relatedAccount = relatedAccount;
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
        this.amount = (amount != null) ? amount.setScale(2) : null;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id) || Objects.equals(transactionReference, that.transactionReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, transactionReference);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", transactionReference='" + transactionReference + '\'' +
                ", transactionType=" + transactionType +
                ", amount=" + amount +
                ", transactionStatus=" + transactionStatus +
                ", description='" + description + '\'' +
                ", transactionDate=" + transactionDate +
                '}';
    }
}
