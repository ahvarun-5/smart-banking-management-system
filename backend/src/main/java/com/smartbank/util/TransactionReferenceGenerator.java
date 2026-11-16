package com.smartbank.util;

import com.smartbank.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class TransactionReferenceGenerator {

    private final TransactionRepository transactionRepository;
    private final SecureRandom random = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    public TransactionReferenceGenerator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Generates a unique transaction reference identifier.
     * Example format: TXN-20261115-K9M2P4X7
     */
    public String generateReference() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String reference;
        int attempts = 0;

        do {
            if (attempts++ > 20) {
                throw new IllegalStateException("Unable to generate a unique transaction reference after multiple attempts");
            }
            StringBuilder randomPart = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                int index = random.nextInt(ALPHANUMERIC.length());
                randomPart.append(ALPHANUMERIC.charAt(index));
            }
            reference = String.format("TXN-%s-%s", datePart, randomPart);
        } while (transactionRepository.existsByTransactionReference(reference));

        return reference;
    }
}
