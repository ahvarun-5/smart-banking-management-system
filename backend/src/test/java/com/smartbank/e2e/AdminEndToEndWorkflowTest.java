package com.smartbank.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbank.dto.*;
import com.smartbank.entity.*;
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
class AdminEndToEndWorkflowTest {

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

    @BeforeEach
    void cleanAndSeedAdmin() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // Seed administrator
        User admin = new User(
                "System Admin",
                "admin@smartbank.com",
                "9999999999",
                passwordEncoder.encode("Admin@1234"),
                "Headquarters",
                Role.ROLE_ADMIN
        );
        userRepository.save(admin);
    }

    @Test
    @DisplayName("Complete Admin End-to-End Workflow: Login -> Metrics -> Customer Search & Inspection -> Deactivate -> Blocked Action -> Reactivate -> Freeze Account -> Blocked Action -> Unfreeze -> System Transaction Audit")
    void testCompleteAdminWorkflow() throws Exception {
        // 1. ADMIN LOGIN & TOKEN EXTRACTION
        LoginRequest adminLogin = new LoginRequest("admin@smartbank.com", "Admin@1234");
        MvcResult adminLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String adminToken = objectMapper.readTree(adminLoginRes.getResponse().getContentAsString()).get("data").get("token").asText();

        // 2. RETRIEVE DASHBOARD ANALYTICS (Initial State)
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(0))
                .andExpect(jsonPath("$.totalAccounts").value(0));

        // 3. REGISTER A TEST CUSTOMER AND OPEN ACCOUNT
        RegisterRequest regReq = new RegisterRequest(
                "Rohan Mehra", "rohan@example.com", "9123456780", "Pune City", "Pass@123", "Pass@123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        MvcResult custLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("rohan@example.com", "Pass@123"))))
                .andExpect(status().isOk())
                .andReturn();

        String customerToken = objectMapper.readTree(custLoginRes.getResponse().getContentAsString()).get("data").get("token").asText();

        MvcResult accRes = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AccountCreateRequest(AccountType.SAVINGS, new BigDecimal("2000.00")))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode accJson = objectMapper.readTree(accRes.getResponse().getContentAsString());
        String accNum = accJson.get("accountNumber").asText();
        Long accId = accJson.get("id").asLong();

        // 4. ADMIN SEARCHES CUSTOMER BY NAME
        MvcResult searchRes = mockMvc.perform(get("/api/admin/customers?query=Rohan")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("rohan@example.com"))
                .andReturn();

        Long customerId = objectMapper.readTree(searchRes.getResponse().getContentAsString())
                .get("content").get(0).get("id").asLong();

        // 5. VIEW FULL CUSTOMER PORTFOLIO DETAILS
        mockMvc.perform(get("/api/admin/customers/" + customerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.totalBalance").value(2000.00))
                .andExpect(jsonPath("$.accounts[0].accountNumber").value(accNum));

        // 6. DEACTIVATE THE CUSTOMER
        CustomerStatusUpdateRequest deactReq = new CustomerStatusUpdateRequest(false, "Suspected identity anomaly");
        mockMvc.perform(patch("/api/admin/customers/" + customerId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // 7. VERIFY DEACTIVATED CUSTOMER BANKING OPERATIONS ARE BLOCKED (HTTP 403)
        DepositRequest blockedDeposit = new DepositRequest(accNum, new BigDecimal("100.00"), "Deposit attempt");
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockedDeposit)))
                .andExpect(status().isForbidden());

        // 8. REACTIVATE THE CUSTOMER
        CustomerStatusUpdateRequest reactReq = new CustomerStatusUpdateRequest(true, "Identity verified successfully");
        mockMvc.perform(patch("/api/admin/customers/" + customerId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reactReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        // 9. FREEZE THE BANK ACCOUNT
        AccountStatusUpdateRequest freezeReq = new AccountStatusUpdateRequest(AccountStatus.FROZEN, "Compliance hold");
        mockMvc.perform(patch("/api/admin/accounts/" + accId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(freezeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("FROZEN"));

        // 10. VERIFY OPERATIONS ON FROZEN ACCOUNT ARE REJECTED (HTTP 400)
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(accNum, new BigDecimal("100.00"), "Deposit on frozen"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("FROZEN")));

        // 11. UNFREEZE / ACTIVATE THE ACCOUNT
        AccountStatusUpdateRequest unfreezeReq = new AccountStatusUpdateRequest(AccountStatus.ACTIVE, "Hold released");
        mockMvc.perform(patch("/api/admin/accounts/" + accId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unfreezeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));

        // 12. VERIFY PERMITTED OPERATION SUCCEEDS NOW
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(accNum, new BigDecimal("100.00"), "Deposit after unfreeze"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.00));

        // 13. INSPECT SYSTEM-WIDE TRANSACTION HISTORY AS ADMIN
        mockMvc.perform(get("/api/admin/transactions?accountNumber=" + accNum)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2)); // Initial 2000 deposit + 100 deposit

        // 14. VERIFY DASHBOARD METRICS REFLECT ACTUAL DB STATE
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(1))
                .andExpect(jsonPath("$.totalActiveCustomers").value(1))
                .andExpect(jsonPath("$.totalAccounts").value(1))
                .andExpect(jsonPath("$.totalActiveAccounts").value(1))
                .andExpect(jsonPath("$.totalAccountBalances").value(2100.00))
                .andExpect(jsonPath("$.totalDepositVolume").value(2100.00));
    }
}
