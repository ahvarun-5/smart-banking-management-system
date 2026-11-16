package com.smartbank.config;

import com.smartbank.entity.*;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin.enabled:true}")
    private boolean adminInitEnabled;

    @Value("${app.init.admin.email:admin@smartbank.com}")
    private String adminEmail;

    @Value("${app.init.admin.password:Admin@1234}")
    private String adminPassword;

    @Value("${app.init.admin.name:System Administrator}")
    private String adminName;

    @Value("${app.init.admin.phone:9999999999}")
    private String adminPhone;

    @Value("${app.init.admin.address:Headquarters, Smart Bank Tower, Mumbai}")
    private String adminAddress;

    public DataInitializer(UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!adminInitEnabled) {
            logger.info("Admin account auto-initialization is disabled by configuration");
            return;
        }

        try {
            // 1. Initialize Administrator
            String normalizedAdminEmail = adminEmail.trim().toLowerCase();
            if (!userRepository.existsByEmail(normalizedAdminEmail)) {
                logger.info("Initializing default administrator account: {}", normalizedAdminEmail);

                User admin = new User(
                        adminName,
                        normalizedAdminEmail,
                        adminPhone,
                        passwordEncoder.encode(adminPassword),
                        adminAddress,
                        Role.ROLE_ADMIN
                );
                userRepository.save(admin);
                logger.info("Default administrator account created successfully [{}]", normalizedAdminEmail);
            } else {
                logger.info("Administrator account already present: {}", normalizedAdminEmail);
            }

            // 2. Initialize Sample Customer: Rahul Sharma
            String rahulEmail = "rahul.sharma@example.com";
            if (!userRepository.existsByEmail(rahulEmail)) {
                logger.info("Initializing sample customer account: {}", rahulEmail);
                User rahul = new User(
                        "Rahul Sharma",
                        rahulEmail,
                        "9812345678",
                        passwordEncoder.encode("Customer@1234"),
                        "Flat 402, Sunshine Apartments, Bengaluru, Karnataka",
                        Role.ROLE_CUSTOMER
                );
                rahul = userRepository.save(rahul);

                Account acc1 = new Account("SB-1002003001", rahul, AccountType.SAVINGS, new BigDecimal("25000.00"), AccountStatus.ACTIVE);
                Account acc2 = new Account("CA-1002003002", rahul, AccountType.CURRENT, new BigDecimal("50000.00"), AccountStatus.ACTIVE);
                accountRepository.save(acc1);
                accountRepository.save(acc2);
                logger.info("Sample customer Rahul Sharma and accounts initialized");
            }

            // 3. Initialize Sample Customer: Priya Patel
            String priyaEmail = "priya.patel@example.com";
            if (!userRepository.existsByEmail(priyaEmail)) {
                logger.info("Initializing sample customer account: {}", priyaEmail);
                User priya = new User(
                        "Priya Patel",
                        priyaEmail,
                        "9823456789",
                        passwordEncoder.encode("Customer@1234"),
                        "A-12 Green Valley, Ahmedabad, Gujarat",
                        Role.ROLE_CUSTOMER
                );
                priya = userRepository.save(priya);

                Account acc3 = new Account("SB-1002003003", priya, AccountType.SAVINGS, new BigDecimal("15000.00"), AccountStatus.ACTIVE);
                accountRepository.save(acc3);
                logger.info("Sample customer Priya Patel and account initialized");
            }

        } catch (Exception ex) {
            logger.warn("Could not complete data initialization: {}", ex.getMessage());
        }
    }
}
