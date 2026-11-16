package com.smartbank.service.impl;

import com.smartbank.dto.DepositRequest;
import com.smartbank.dto.TransactionResponse;
import com.smartbank.dto.TransferRequest;
import com.smartbank.dto.WithdrawRequest;
import com.smartbank.entity.*;
import com.smartbank.exception.AccountInactiveException;
import com.smartbank.exception.InsufficientBalanceException;
import com.smartbank.exception.InvalidTransactionException;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.exception.UnauthorizedAccessException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.TransactionRepository;
import com.smartbank.repository.UserRepository;
import com.smartbank.service.TransactionService;
import com.smartbank.util.TransactionReferenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionReferenceGenerator referenceGenerator;

    public TransactionServiceImpl(AccountRepository accountRepository,
                                  TransactionRepository transactionRepository,
                                  UserRepository userRepository,
                                  TransactionReferenceGenerator referenceGenerator) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.referenceGenerator = referenceGenerator;
    }

    @Override
    @Transactional
    public TransactionResponse deposit(DepositRequest request, String userEmail) {
        log.info("Processing deposit to account: {} by user: {}", request.getAccountNumber(), userEmail);

        validateAmount(request.getAmount());

        User user = getUser(userEmail);

        // Pessimistic write lock on target account
        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + request.getAccountNumber()));

        verifyAccountOwnership(account, user);
        verifyAccountIsActive(account);

        BigDecimal depositAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        account.setBalance(account.getBalance().add(depositAmount));
        accountRepository.save(account);

        String reference = referenceGenerator.generateReference();
        String description = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                ? request.getDescription().trim()
                : "Deposit into account " + account.getAccountNumber();

        Transaction transaction = new Transaction(
                reference,
                account,
                null,
                TransactionType.DEPOSIT,
                depositAmount,
                TransactionStatus.SUCCESS,
                description
        );

        Transaction savedTx = transactionRepository.save(transaction);
        log.info("Deposit of {} to account {} successful. Reference: {}", depositAmount, account.getAccountNumber(), reference);

        return TransactionResponse.fromEntity(savedTx);
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request, String userEmail) {
        log.info("Processing withdrawal from account: {} by user: {}", request.getAccountNumber(), userEmail);

        validateAmount(request.getAmount());

        User user = getUser(userEmail);

        // Pessimistic write lock on target account
        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + request.getAccountNumber()));

        verifyAccountOwnership(account, user);
        verifyAccountIsActive(account);

        BigDecimal withdrawAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        if (account.getBalance().compareTo(withdrawAmount) < 0) {
            log.warn("Withdrawal failed: insufficient balance for account {}. Current: {}, Requested: {}",
                    account.getAccountNumber(), account.getBalance(), withdrawAmount);
            throw new InsufficientBalanceException("Insufficient funds. Available balance: " + account.getBalance() + ", requested: " + withdrawAmount);
        }

        account.setBalance(account.getBalance().subtract(withdrawAmount));
        accountRepository.save(account);

        String reference = referenceGenerator.generateReference();
        String description = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                ? request.getDescription().trim()
                : "Withdrawal from account " + account.getAccountNumber();

        Transaction transaction = new Transaction(
                reference,
                account,
                null,
                TransactionType.WITHDRAWAL,
                withdrawAmount,
                TransactionStatus.SUCCESS,
                description
        );

        Transaction savedTx = transactionRepository.save(transaction);
        log.info("Withdrawal of {} from account {} successful. Reference: {}", withdrawAmount, account.getAccountNumber(), reference);

        return TransactionResponse.fromEntity(savedTx);
    }

    @Override
    @Transactional
    public TransactionResponse transfer(TransferRequest request, String userEmail) {
        log.info("Processing transfer from {} to {} by user: {}",
                request.getSenderAccountNumber(), request.getReceiverAccountNumber(), userEmail);

        if (request.getSenderAccountNumber().equalsIgnoreCase(request.getReceiverAccountNumber())) {
            throw new InvalidTransactionException("Sender and receiver accounts cannot be the same");
        }

        validateAmount(request.getAmount());

        User user = getUser(userEmail);

        // Deterministic lock ordering based on account numbers to prevent deadlocks
        Account senderAccount;
        Account receiverAccount;

        if (request.getSenderAccountNumber().compareTo(request.getReceiverAccountNumber()) < 0) {
            senderAccount = accountRepository.findByAccountNumberWithLock(request.getSenderAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Sender account not found: " + request.getSenderAccountNumber()));
            receiverAccount = accountRepository.findByAccountNumberWithLock(request.getReceiverAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found: " + request.getReceiverAccountNumber()));
        } else {
            receiverAccount = accountRepository.findByAccountNumberWithLock(request.getReceiverAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found: " + request.getReceiverAccountNumber()));
            senderAccount = accountRepository.findByAccountNumberWithLock(request.getSenderAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Sender account not found: " + request.getSenderAccountNumber()));
        }

        verifyAccountOwnership(senderAccount, user);
        verifyAccountIsActive(senderAccount);
        verifyAccountIsActive(receiverAccount);

        BigDecimal transferAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        if (senderAccount.getBalance().compareTo(transferAmount) < 0) {
            log.warn("Transfer failed: insufficient balance in sender account {}. Current: {}, Required: {}",
                    senderAccount.getAccountNumber(), senderAccount.getBalance(), transferAmount);
            throw new InsufficientBalanceException("Insufficient funds for transfer. Available: " + senderAccount.getBalance() + ", required: " + transferAmount);
        }

        // Perform balance updates atomically
        senderAccount.setBalance(senderAccount.getBalance().subtract(transferAmount));
        receiverAccount.setBalance(receiverAccount.getBalance().add(transferAmount));

        accountRepository.save(senderAccount);
        accountRepository.save(receiverAccount);

        // Record transactions for both parties
        String senderRef = referenceGenerator.generateReference();
        String receiverRef = referenceGenerator.generateReference();

        String baseDesc = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                ? request.getDescription().trim()
                : "Fund transfer";

        Transaction senderTx = new Transaction(
                senderRef,
                senderAccount,
                receiverAccount,
                TransactionType.TRANSFER_OUT,
                transferAmount,
                TransactionStatus.SUCCESS,
                baseDesc + " to account " + receiverAccount.getAccountNumber()
        );

        Transaction receiverTx = new Transaction(
                receiverRef,
                receiverAccount,
                senderAccount,
                TransactionType.TRANSFER_IN,
                transferAmount,
                TransactionStatus.SUCCESS,
                baseDesc + " from account " + senderAccount.getAccountNumber()
        );

        Transaction savedSenderTx = transactionRepository.save(senderTx);
        transactionRepository.save(receiverTx);

        log.info("Transfer of {} from {} to {} completed successfully. SenderRef: {}, ReceiverRef: {}",
                transferAmount, senderAccount.getAccountNumber(), receiverAccount.getAccountNumber(), senderRef, receiverRef);

        return TransactionResponse.fromEntity(savedSenderTx);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getCustomerTransactions(
            String userEmail,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String reference,
            Pageable pageable) {

        User user = getUser(userEmail);
        List<Account> accounts = accountRepository.findByUserId(user.getId());

        if (accounts.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Long> accountIds = accounts.stream().map(Account::getId).toList();

        return transactionRepository.filterCustomerTransactions(
                accountIds,
                type,
                startDate,
                endDate,
                reference != null && !reference.trim().isEmpty() ? reference.trim() : null,
                pageable
        ).map(TransactionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAccountTransactions(
            String accountNumber,
            String userEmail,
            Pageable pageable) {

        User user = getUser(userEmail);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        verifyAccountOwnership(account, user);

        return transactionRepository.findByAccountId(account.getId(), pageable)
                .map(TransactionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String reference, String userEmail) {
        User user = getUser(userEmail);

        Transaction tx = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference: " + reference));

        boolean isPrimaryOwner = tx.getAccount().getUser().getId().equals(user.getId());
        boolean isRelatedOwner = tx.getRelatedAccount() != null && tx.getRelatedAccount().getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isPrimaryOwner && !isRelatedOwner && !isAdmin) {
            log.warn("Unauthorized transaction inquiry: user {} tried to view reference {}", user.getEmail(), reference);
            throw new UnauthorizedAccessException("You do not have permission to view this transaction");
        }

        return TransactionResponse.fromEntity(tx);
    }

    private User getUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        if (!user.isEnabled()) {
            log.warn("Access rejected: user account {} is deactivated", email);
            throw new UnauthorizedAccessException("User account is deactivated. Financial operations are disabled.");
        }
        return user;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be strictly positive");
        }
    }

    private void verifyAccountOwnership(Account account, User user) {
        if (user.getRole() != Role.ROLE_ADMIN && !account.getUser().getId().equals(user.getId())) {
            log.warn("Ownership violation: user {} does not own account {}", user.getEmail(), account.getAccountNumber());
            throw new UnauthorizedAccessException("You do not have access to this account");
        }
    }

    private void verifyAccountIsActive(Account account) {
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            log.warn("Operation rejected: account {} is in status {}", account.getAccountNumber(), account.getAccountStatus());
            throw new AccountInactiveException("Account " + account.getAccountNumber() + " is " + account.getAccountStatus() + ". Operations cannot be performed.");
        }
    }
}
