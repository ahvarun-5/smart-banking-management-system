package com.smartbank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbank.dto.AccountCreateRequest;
import com.smartbank.dto.AccountResponse;
import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.AccountType;
import com.smartbank.security.CustomUserDetailsService;
import com.smartbank.security.JwtService;
import com.smartbank.security.TokenBlacklistService;
import com.smartbank.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/accounts - 201 Created on valid account creation")
    void testCreateAccountSuccess() throws Exception {
        AccountCreateRequest request = new AccountCreateRequest(AccountType.SAVINGS, new BigDecimal("1000.00"));
        AccountResponse response = new AccountResponse(
                1L, "SB-1001234567", AccountType.SAVINGS, new BigDecimal("1000.00"),
                AccountStatus.ACTIVE, LocalDateTime.now(), "Customer User", "customer@smartbank.com"
        );

        when(accountService.createAccount(any(AccountCreateRequest.class), eq("customer@smartbank.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("SB-1001234567"))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"));
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/accounts - 400 Bad Request when account type is missing")
    void testCreateAccountInvalidInput() throws Exception {
        AccountCreateRequest request = new AccountCreateRequest(null, new BigDecimal("500.00"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/accounts - 401 Unauthorized when unauthenticated")
    void testCreateAccountUnauthenticated() throws Exception {
        AccountCreateRequest request = new AccountCreateRequest(AccountType.SAVINGS, new BigDecimal("500.00"));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/accounts - 200 OK returns user's account list")
    void testGetMyAccounts() throws Exception {
        AccountResponse acc = new AccountResponse(
                1L, "SB-1001234567", AccountType.SAVINGS, new BigDecimal("1000.00"),
                AccountStatus.ACTIVE, LocalDateTime.now(), "Customer User", "customer@smartbank.com"
        );

        when(accountService.getAccountsByUser("customer@smartbank.com")).thenReturn(List.of(acc));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").value("SB-1001234567"))
                .andExpect(jsonPath("$[0].balance").value(1000.00));
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/accounts/number/{accountNumber} - 200 OK returns single account")
    void testGetAccountByNumber() throws Exception {
        AccountResponse acc = new AccountResponse(
                1L, "SB-1001234567", AccountType.SAVINGS, new BigDecimal("1000.00"),
                AccountStatus.ACTIVE, LocalDateTime.now(), "Customer User", "customer@smartbank.com"
        );

        when(accountService.getAccountByNumber("SB-1001234567", "customer@smartbank.com")).thenReturn(acc);

        mockMvc.perform(get("/api/accounts/number/SB-1001234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("SB-1001234567"));
    }
}
