package com.smartbank.repository;

import com.smartbank.entity.Account;
import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUserId(Long userId);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    Optional<Account> findByAccountNumberAndUserId(String accountNumber, Long userId);

    /**
     * Pessimistic write lock for atomic financial operations (deposits, withdrawals, transfers)
     * preventing race conditions and double-spending across concurrent threads.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    long countByAccountStatus(AccountStatus accountStatus);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a")
    BigDecimal sumTotalBalance();

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.user.id = :userId")
    BigDecimal sumBalanceByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);

    @Query("SELECT a FROM Account a WHERE " +
           "a.accountNumber LIKE CONCAT('%', :query, '%') OR " +
           "LOWER(a.user.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.user.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Account> searchAccounts(@Param("query") String query, Pageable pageable);

    @Query("SELECT a FROM Account a WHERE " +
           "(:type IS NULL OR a.accountType = :type) AND " +
           "(:status IS NULL OR a.accountStatus = :status) AND " +
           "(:query IS NULL OR (" +
           "    a.accountNumber LIKE CONCAT('%', :query, '%') OR " +
           "    LOWER(a.user.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "    LOWER(a.user.email) LIKE LOWER(CONCAT('%', :query, '%'))))")
    Page<Account> filterAdminAccounts(
            @Param("type") AccountType type,
            @Param("status") AccountStatus status,
            @Param("query") String query,
            Pageable pageable
    );
}
