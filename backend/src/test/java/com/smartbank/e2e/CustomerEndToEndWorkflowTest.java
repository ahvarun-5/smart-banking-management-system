package com.smartbank.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbank.dto.*;
import com.smartbank.entity.AccountType;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.TransactionRepository;
import com.smartbank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class CustomerEndToEndWorkflowTest {

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

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete Customer End-to-End Workflow: Register -> Login -> Account Opening -> Deposit -> Withdraw -> Transfer -> History -> Profile -> Password Change")
    void testCompleteCustomerWorkflow() throws Exception {
        // 1. REGISTER CUSTOMER A
        RegisterRequest registerReq = new RegisterRequest(
                "Aarav Patel",
                "aarav@example.com",
                "9876543210",
                "45 Marine Drive, Mumbai",
                "Secret@123",
                "Secret@123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.email").value("aarav@example.com"))
                .andExpect(jsonPath("$.data.user.fullName").value("Aarav Patel"));

        // 2. LOGIN CUSTOMER A & EXTRACT JWT
        LoginRequest loginReq = new LoginRequest("aarav@example.com", "Secret@123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String tokenA = loginNode.get("data").get("token").asText();
        assertThat(tokenA).isNotBlank();

        // 3. RETRIEVE CUSTOMER PROFILE
        mockMvc.perform(get("/api/customers/profile")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("aarav@example.com"))
                .andExpect(jsonPath("$.fullName").value("Aarav Patel"));

        // 4. OPEN A NEW SAVINGS ACCOUNT WITH INITIAL DEPOSIT $1000.00
        AccountCreateRequest accReq = new AccountCreateRequest(AccountType.SAVINGS, new BigDecimal("1000.00"));
        MvcResult accResult = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").isNotEmpty())
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andReturn();

        JsonNode accNode = objectMapper.readTree(accResult.getResponse().getContentAsString());
        String accountA = accNode.get("accountNumber").asText();
        assertThat(accountA).startsWith("SB-");

        // 5. DEPOSIT $500.00
        DepositRequest depositReq = new DepositRequest(accountA, new BigDecimal("500.00"), "Bonus payment");
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(500.00));

        // 6. VERIFY UPDATED BALANCE ($1000 + $500 = $1500)
        mockMvc.perform(get("/api/accounts/number/" + accountA)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.00));

        // 7. WITHDRAW $200.00
        WithdrawRequest withdrawReq = new WithdrawRequest(accountA, new BigDecimal("200.00"), "ATM cash withdrawal");
        mockMvc.perform(post("/api/transactions/withdraw")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(200.00));

        // 8. VERIFY UPDATED BALANCE ($1500 - $200 = $1300)
        mockMvc.perform(get("/api/accounts/number/" + accountA)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1300.00));

        // 9. REGISTER CUSTOMER B AND OPEN ACCOUNT B
        RegisterRequest registerReqB = new RegisterRequest(
                "Diya Sharma",
                "diya@example.com",
                "9876543211",
                "12 Park Street, Kolkata",
                "Secret@456",
                "Secret@456"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReqB)))
                .andExpect(status().isCreated());

        MvcResult loginResultB = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("diya@example.com", "Secret@456"))))
                .andExpect(status().isOk())
                .andReturn();

        String tokenB = objectMapper.readTree(loginResultB.getResponse().getContentAsString()).get("data").get("token").asText();

        MvcResult accResultB = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AccountCreateRequest(AccountType.CURRENT, new BigDecimal("200.00")))))
                .andExpect(status().isCreated())
                .andReturn();

        String accountB = objectMapper.readTree(accResultB.getResponse().getContentAsString()).get("accountNumber").asText();

        // 10. TRANSFER $300.00 FROM A TO B
        TransferRequest transferReq = new TransferRequest(accountA, accountB, new BigDecimal("300.00"), "Project fee split");
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.amount").value(300.00));

        // 11. VERIFY BALANCES POST-TRANSFER (A: $1300 - $300 = $1000; B: $200 + $300 = $500)
        mockMvc.perform(get("/api/accounts/number/" + accountA)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000.00));

        mockMvc.perform(get("/api/accounts/number/" + accountB)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));

        // 12. RETRIEVE AND AUDIT TRANSACTION HISTORY
        MvcResult txHistoryResult = mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();

        JsonNode txHistory = objectMapper.readTree(txHistoryResult.getResponse().getContentAsString()).get("content");
        // Customer A should have 4 transactions: Initial deposit (1000), Deposit (500), Withdrawal (200), Transfer Out (300)
        assertThat(txHistory.size()).isEqualTo(4);

        // 13. UPDATE PROFILE
        CustomerProfileUpdateRequest profileUpdateReq = new CustomerProfileUpdateRequest(
                "Aarav K. Patel",
                "9911223344",
                "100 Altamount Road, Mumbai"
        );
        mockMvc.perform(put("/api/customers/profile")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileUpdateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Aarav K. Patel"))
                .andExpect(jsonPath("$.phoneNumber").value("9911223344"));

        // 14. CHANGE PASSWORD
        ChangePasswordRequest pwdReq = new ChangePasswordRequest(
                "Secret@123",
                "NewSecret@999",
                "NewSecret@999"
        );
        mockMvc.perform(put("/api/customers/change-password")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pwdReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 15. VERIFY OLD PASSWORD REJECTED
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("aarav@example.com", "Secret@123"))))
                .andExpect(status().isUnauthorized());

        // 16. VERIFY NEW PASSWORD LOGS IN SUCCESSFULLY
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("aarav@example.com", "NewSecret@999"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }
}
