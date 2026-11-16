package com.smartbank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbank.dto.DepositRequest;
import com.smartbank.dto.TransactionResponse;
import com.smartbank.dto.TransferRequest;
import com.smartbank.dto.WithdrawRequest;
import com.smartbank.entity.TransactionStatus;
import com.smartbank.entity.TransactionType;
import com.smartbank.exception.InsufficientBalanceException;
import com.smartbank.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/transactions/deposit - 200 OK on valid deposit")
    void testDepositSuccess() throws Exception {
        DepositRequest request = new DepositRequest("SB-1001", new BigDecimal("500.00"), "Deposit");
        TransactionResponse response = new TransactionResponse(
                1L, "TXN-DEP-01", "SB-1001", null,
                TransactionType.DEPOSIT, new BigDecimal("500.00"),
                TransactionStatus.SUCCESS, "Deposit", LocalDateTime.now()
        );

        when(transactionService.deposit(any(DepositRequest.class), eq("customer@smartbank.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").value("TXN-DEP-01"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"));
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/transactions/withdraw - 200 OK on valid withdrawal")
    void testWithdrawSuccess() throws Exception {
        WithdrawRequest request = new WithdrawRequest("SB-1001", new BigDecimal("200.00"), "Withdrawal");
        TransactionResponse response = new TransactionResponse(
                2L, "TXN-WTH-01", "SB-1001", null,
                TransactionType.WITHDRAWAL, new BigDecimal("200.00"),
                TransactionStatus.SUCCESS, "Withdrawal", LocalDateTime.now()
        );

        when(transactionService.withdraw(any(WithdrawRequest.class), eq("customer@smartbank.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").value("TXN-WTH-01"))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"));
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/transactions/withdraw - 400 Bad Request when balance is insufficient")
    void testWithdrawInsufficientBalance() throws Exception {
        WithdrawRequest request = new WithdrawRequest("SB-1001", new BigDecimal("9999.00"), "Excess withdrawal");

        when(transactionService.withdraw(any(WithdrawRequest.class), eq("customer@smartbank.com")))
                .thenThrow(new InsufficientBalanceException("Insufficient funds"));

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("POST /api/transactions/transfer - 200 OK on valid transfer")
    void testTransferSuccess() throws Exception {
        TransferRequest request = new TransferRequest("SB-1001", "CA-2002", new BigDecimal("300.00"), "Bill Split");
        TransactionResponse response = new TransactionResponse(
                3L, "TXN-TRF-01", "SB-1001", "CA-2002",
                TransactionType.TRANSFER_OUT, new BigDecimal("300.00"),
                TransactionStatus.SUCCESS, "Bill Split", LocalDateTime.now()
        );

        when(transactionService.transfer(any(TransferRequest.class), eq("customer@smartbank.com")))
                .thenReturn(response);

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").value("TXN-TRF-01"))
                .andExpect(jsonPath("$.amount").value(300.00))
                .andExpect(jsonPath("$.transactionType").value("TRANSFER_OUT"));
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/transactions - 200 OK returns paginated transaction history")
    void testGetCustomerTransactions() throws Exception {
        TransactionResponse tx = new TransactionResponse(
                1L, "TXN-DEP-01", "SB-1001", null,
                TransactionType.DEPOSIT, new BigDecimal("500.00"),
                TransactionStatus.SUCCESS, "Deposit", LocalDateTime.now()
        );

        when(transactionService.getCustomerTransactions(eq("customer@smartbank.com"), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionReference").value("TXN-DEP-01"));
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/transactions/{reference} - 200 OK returns transaction detail")
    void testGetTransactionByReference() throws Exception {
        TransactionResponse tx = new TransactionResponse(
                1L, "TXN-DEP-01", "SB-1001", null,
                TransactionType.DEPOSIT, new BigDecimal("500.00"),
                TransactionStatus.SUCCESS, "Deposit", LocalDateTime.now()
        );

        when(transactionService.getTransactionByReference("TXN-DEP-01", "customer@smartbank.com"))
                .thenReturn(tx);

        mockMvc.perform(get("/api/transactions/TXN-DEP-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").value("TXN-DEP-01"));
    }

    @Test
    @DisplayName("POST /api/transactions/deposit - 401 Unauthorized when unauthenticated")
    void testDepositUnauthenticated() throws Exception {
        DepositRequest request = new DepositRequest("SB-1001", new BigDecimal("500.00"), "Deposit");

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
