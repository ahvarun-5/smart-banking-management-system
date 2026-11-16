package com.smartbank.repository;

import com.smartbank.entity.Transaction;
import com.smartbank.entity.TransactionStatus;
import com.smartbank.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    boolean existsByTransactionReference(String transactionReference);

    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    Page<Transaction> findByAccountIdIn(List<Long> accountIds, Pageable pageable);

    List<Transaction> findTop5ByOrderByTransactionDateDesc();

    List<Transaction> findTop10ByOrderByTransactionDateDesc();

    List<Transaction> findTop5ByAccountIdInOrderByTransactionDateDesc(List<Long> accountIds);

    @Query("SELECT t FROM Transaction t WHERE t.account.id IN :accountIds " +
           "AND (:type IS NULL OR t.transactionType = :type) " +
           "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
           "AND (:reference IS NULL OR LOWER(t.transactionReference) LIKE LOWER(CONCAT('%', :reference, '%')))")
    Page<Transaction> filterCustomerTransactions(
            @Param("accountIds") List<Long> accountIds,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("reference") String reference,
            Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:type IS NULL OR t.transactionType = :type) " +
           "AND (:status IS NULL OR t.transactionStatus = :status) " +
           "AND (:accountNumber IS NULL OR t.account.accountNumber = :accountNumber) " +
           "AND (:customerQuery IS NULL OR (" +
           "    LOWER(t.account.user.fullName) LIKE LOWER(CONCAT('%', :customerQuery, '%')) OR " +
           "    LOWER(t.account.user.email) LIKE LOWER(CONCAT('%', :customerQuery, '%')))) " +
           "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
           "AND (:reference IS NULL OR LOWER(t.transactionReference) LIKE LOWER(CONCAT('%', :reference, '%')))")
    Page<Transaction> filterAdminTransactions(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("accountNumber") String accountNumber,
            @Param("customerQuery") String customerQuery,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("reference") String reference,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.transactionType = 'DEPOSIT' AND t.transactionStatus = 'SUCCESS'")
    BigDecimal sumTotalDeposits();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.transactionType = 'WITHDRAWAL' AND t.transactionStatus = 'SUCCESS'")
    BigDecimal sumTotalWithdrawals();

    /**
     * Total transfer volume measured via TRANSFER_OUT records with SUCCESS.
     * Counting only TRANSFER_OUT prevents double-counting, as every internal transfer
     * produces exactly one TRANSFER_OUT and one paired TRANSFER_IN of identical amount.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.transactionType = 'TRANSFER_OUT' AND t.transactionStatus = 'SUCCESS'")
    BigDecimal sumTotalTransfers();
}
