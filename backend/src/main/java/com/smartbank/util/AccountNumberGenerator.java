package com.smartbank.util;

import com.smartbank.entity.AccountType;
import com.smartbank.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private final AccountRepository accountRepository;
    private final SecureRandom random = new SecureRandom();

    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Generates a unique, formatted bank account number.
     * Format: SB-100XXXXXXX for SAVINGS, CA-200XXXXXXX for CURRENT.
     */
    public String generateAccountNumber(AccountType accountType) {
        String prefix = (accountType == AccountType.SAVINGS) ? "SB" : "CA";
        int typeDigit = (accountType == AccountType.SAVINGS) ? 1 : 2;

        String accountNumber;
        int attempts = 0;
        do {
            if (attempts++ > 20) {
                throw new IllegalStateException("Unable to generate a unique account number after multiple attempts");
            }
            // Generate a 9-digit random number prefixed with typeDigit
            long randomDigits = 100_000_000L + (Math.abs(random.nextLong()) % 900_000_000L);
            accountNumber = String.format("%s-%d%08d", prefix, typeDigit, randomDigits % 100_000_000L);
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}
