package com.smartbank.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbank.dto.*;
import com.smartbank.entity.*;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.TransactionRepository;
import com.smartbank.repository.UserRepository;
import com.smartbank.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class SecurityAndValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User customer1;
    private User customer2;
    private Account account1;
    private Account account2;
    private String token1;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        customer1 = new User("Customer One", "user1@example.com", "9811111111", passwordEncoder.encode("Pass@123"), "Mumbai", Role.ROLE_CUSTOMER);
        customer2 = new User("Customer Two", "user2@example.com", "9822222222", passwordEncoder.encode("Pass@456"), "Delhi", Role.ROLE_CUSTOMER);

        customer1 = userRepository.save(customer1);
        customer2 = userRepository.save(customer2);

        account1 = new Account("SB-1001", customer1, AccountType.SAVINGS, new BigDecimal("500.00"), AccountStatus.ACTIVE);
        account2 = new Account("CA-2002", customer2, AccountType.CURRENT, new BigDecimal("1000.00"), AccountStatus.ACTIVE);

        account1 = accountRepository.save(account1);
        account2 = accountRepository.save(account2);

        java.util.Map<String, Object> claims = java.util.Map.of("userId", customer1.getId(), "role", customer1.getRole().name());
        token1 = jwtService.generateToken(claims, customer1.getEmail());
    }

    @Test
    @DisplayName("Security - Unauthenticated users cannot access protected banking endpoints")
    void testUnauthenticatedAccessBlocked() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/customers/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security - Customers cannot access administrative endpoints (403 Forbidden)")
    void testCustomerAttemptingAdminEndpointsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/accounts")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security - Customers cannot access other customers' accounts (403 Forbidden)")
    void testCrossCustomerAccountAccessForbidden() throws Exception {
        // Customer 1 attempting to view Customer 2's account
        mockMvc.perform(get("/api/accounts/number/CA-2002")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/accounts/" + account2.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security - Customer attempting to deposit or withdraw from unowned account is rejected (403 Forbidden)")
    void testCrossCustomerAccountTransactionForbidden() throws Exception {
        DepositRequest req = new DepositRequest("CA-2002", new BigDecimal("100.00"), "Deposit to someone else");
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security - Malformed or fake JWT tokens are strictly rejected (401)")
    void testInvalidJwtRejected() throws Exception {
        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer invalid.fake.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security - Passwords are saved as BCrypt hashes and never returned in API responses")
    void testPasswordSecurity() throws Exception {
        User user = userRepository.findByEmail("user1@example.com").orElseThrow();
        assertThat(user.getPassword()).startsWith("$2a$10$");
        assertThat(user.getPassword()).doesNotContain("Pass@123");

        // Profile response must never include password field
        MvcResult result = mockMvc.perform(get("/api/customers/profile")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("$2a$10$");
    }

    @Test
    @DisplayName("Security - Duplicate email registration is rejected with 409 Conflict")
    void testDuplicateEmailRegistrationRejected() throws Exception {
        RegisterRequest dupReq = new RegisterRequest(
                "Duplicate User", "user1@example.com", "9800000000", "City of Mumbai", "Pass@123", "Pass@123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // VALIDATION TESTS

    @Test
    @DisplayName("Validation - Negative deposit amounts return 400 Bad Request")
    void testNegativeDepositRejected() throws Exception {
        DepositRequest req = new DepositRequest("SB-1001", new BigDecimal("-50.00"), "Negative");
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation - Zero withdrawal amount returns 400 Bad Request")
    void testZeroWithdrawalRejected() throws Exception {
        WithdrawRequest req = new WithdrawRequest("SB-1001", BigDecimal.ZERO, "Zero");
        mockMvc.perform(post("/api/transactions/withdraw")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation - Withdrawal exceeding balance returns 400 InsufficientBalanceException")
    void testExcessiveWithdrawalRejected() throws Exception {
        WithdrawRequest req = new WithdrawRequest("SB-1001", new BigDecimal("5000.00"), "Excess");
        mockMvc.perform(post("/api/transactions/withdraw")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient funds")));
    }

    @Test
    @DisplayName("Validation - Transfer to same account returns 400 InvalidTransactionException")
    void testTransferToSameAccountRejected() throws Exception {
        TransferRequest req = new TransferRequest("SB-1001", "SB-1001", new BigDecimal("50.00"), "Self transfer");
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot be the same")));
    }

    @Test
    @DisplayName("Validation - Missing required fields return 400 Bad Request")
    void testMissingRequiredFieldsRejected() throws Exception {
        RegisterRequest invalidReq = new RegisterRequest(
                "", "", "", "", "", ""
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }
}
