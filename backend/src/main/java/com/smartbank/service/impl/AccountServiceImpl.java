package com.smartbank.service.impl;

import com.smartbank.dto.AccountCreateRequest;
import com.smartbank.dto.AccountResponse;
import com.smartbank.entity.*;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.exception.UnauthorizedAccessException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.TransactionRepository;
import com.smartbank.repository.UserRepository;
import com.smartbank.service.AccountService;
import com.smartbank.util.AccountNumberGenerator;
import com.smartbank.util.TransactionReferenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final TransactionReferenceGenerator transactionReferenceGenerator;

    public AccountServiceImpl(AccountRepository accountRepository,
                              UserRepository userRepository,
                              TransactionRepository transactionRepository,
                              AccountNumberGenerator accountNumberGenerator,
                              TransactionReferenceGenerator transactionReferenceGenerator) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.transactionReferenceGenerator = transactionReferenceGenerator;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request, String userEmail) {
        log.info("Creating {} account for user: {}", request.getAccountType(), userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (!user.isEnabled()) {
            throw new UnauthorizedAccessException("User account is disabled. Cannot open bank account.");
        }

        String accountNumber = accountNumberGenerator.generateAccountNumber(request.getAccountType());

        BigDecimal initialDeposit = request.getInitialDeposit() != null
                ? request.getInitialDeposit().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        Account account = new Account(
                accountNumber,
                user,
                request.getAccountType(),
                initialDeposit,
                AccountStatus.ACTIVE
        );

        Account savedAccount = accountRepository.save(account);

        // Record initial deposit transaction if amount > 0
        if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            String txRef = transactionReferenceGenerator.generateReference();

            Transaction depositTx = new Transaction(
                    txRef,
                    savedAccount,
                    null,
                    TransactionType.DEPOSIT,
                    initialDeposit,
                    TransactionStatus.SUCCESS,
                    "Initial deposit on account opening"
            );
            transactionRepository.save(depositTx);
        }

        log.info("Successfully created account {} for user {}", savedAccount.getAccountNumber(), userEmail);
        return AccountResponse.fromEntity(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return accountRepository.findByUserId(user.getId())
                .stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        verifyAccountOwnership(account, user);

        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        verifyAccountOwnership(account, user);

        return AccountResponse.fromEntity(account);
    }

    private void verifyAccountOwnership(Account account, User user) {
        if (user.getRole() != Role.ROLE_ADMIN && !account.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized access attempt: user {} tried to access account {}", user.getEmail(), account.getAccountNumber());
            throw new UnauthorizedAccessException("You do not have permission to access this account");
        }
    }
}
