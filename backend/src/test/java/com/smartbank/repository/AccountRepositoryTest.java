package com.smartbank.repository;

import com.smartbank.entity.Account;
import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.AccountType;
import com.smartbank.entity.Role;
import com.smartbank.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class AccountRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(
                "Jane Doe",
                "jane.doe@example.com",
                "9811223344",
                "$2a$10$hashedpasswordstringsample",
                "456 Park Avenue, Metropolis",
                Role.ROLE_CUSTOMER
        );
        sampleUser = userRepository.save(sampleUser);
    }

    @Test
    @DisplayName("Should create account with active status and retrieve by account number")
    void testSaveAndFindByAccountNumber() {
        Account account = new Account(
                "SB-1002003001",
                sampleUser,
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                AccountStatus.ACTIVE
        );
        Account saved = accountRepository.save(account);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getBalance()).isEqualByComparingTo("5000.00");

        Optional<Account> found = accountRepository.findByAccountNumber("SB-1002003001");
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("jane.doe@example.com");
    }

    @Test
    @DisplayName("Should retrieve account by account number and user id")
    void testFindByAccountNumberAndUserId() {
        Account account = new Account(
                "CA-1002003002",
                sampleUser,
                AccountType.CURRENT,
                new BigDecimal("12000.00"),
                AccountStatus.ACTIVE
        );
        accountRepository.save(account);

        Optional<Account> found = accountRepository.findByAccountNumberAndUserId("CA-1002003002", sampleUser.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAccountType()).isEqualTo(AccountType.CURRENT);
        assertThat(found.get().getBalance()).isEqualByComparingTo("12000.00");
    }

    @Test
    @DisplayName("Should sum balance by user and total balance across system")
    void testAggregateBalances() {
        Account acc1 = new Account(
                "SB-1002003001",
                sampleUser,
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                AccountStatus.ACTIVE
        );
        Account acc2 = new Account(
                "CA-1002003002",
                sampleUser,
                AccountType.CURRENT,
                new BigDecimal("15000.00"),
                AccountStatus.ACTIVE
        );
        accountRepository.save(acc1);
        accountRepository.save(acc2);

        BigDecimal userBalance = accountRepository.sumBalanceByUserId(sampleUser.getId());
        BigDecimal totalSystemBalance = accountRepository.sumTotalBalance();

        assertThat(userBalance).isEqualByComparingTo("20000.00");
        assertThat(totalSystemBalance).isEqualByComparingTo("20000.00");

        List<Account> userAccounts = accountRepository.findByUserId(sampleUser.getId());
        assertThat(userAccounts).hasSize(2);
    }

    @Test
    @DisplayName("Should filter accounts by type, status, and query for admin")
    void testFilterAdminAccounts() {
        Account acc1 = new Account(
                "SB-1002003001",
                sampleUser,
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                AccountStatus.ACTIVE
        );
        Account acc2 = new Account(
                "CA-1002003002",
                sampleUser,
                AccountType.CURRENT,
                new BigDecimal("15000.00"),
                AccountStatus.FROZEN
        );
        accountRepository.save(acc1);
        accountRepository.save(acc2);

        Page<Account> savingsActive = accountRepository.filterAdminAccounts(
                AccountType.SAVINGS, AccountStatus.ACTIVE, "1002", org.springframework.data.domain.PageRequest.of(0, 10)
        );
        assertThat(savingsActive.getContent()).hasSize(1);
        assertThat(savingsActive.getContent().get(0).getAccountNumber()).isEqualTo("SB-1002003001");

        Page<Account> frozenAccounts = accountRepository.filterAdminAccounts(
                null, AccountStatus.FROZEN, null, org.springframework.data.domain.PageRequest.of(0, 10)
        );
        assertThat(frozenAccounts.getContent()).hasSize(1);
        assertThat(frozenAccounts.getContent().get(0).getAccountNumber()).isEqualTo("CA-1002003002");
    }
}
