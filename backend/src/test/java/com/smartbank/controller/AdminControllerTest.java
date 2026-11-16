package com.smartbank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbank.dto.*;
import com.smartbank.entity.AccountStatus;
import com.smartbank.entity.AccountType;
import com.smartbank.entity.Role;
import com.smartbank.entity.TransactionStatus;
import com.smartbank.entity.TransactionType;
import com.smartbank.service.AdminService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @Test
    @WithMockUser(username = "admin@smartbank.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/dashboard - 200 OK for ADMIN role")
    void testAdminDashboardAccessSuccess() throws Exception {
        AdminDashboardResponse response = new AdminDashboardResponse(
                50L, 45L, 60L, 55L,
                new BigDecimal("250000.00"), new BigDecimal("100000.00"),
                new BigDecimal("40000.00"), new BigDecimal("30000.00"),
                300L, List.of()
        );

        when(adminService.getDashboardAnalytics()).thenReturn(response);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(50))
                .andExpect(jsonPath("$.totalAccounts").value(60))
                .andExpect(jsonPath("$.totalAccountBalances").value(250000.00));
    }

    @Test
    @WithMockUser(username = "admin@smartbank.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/customers - 200 OK lists registered customers")
    void testAdminCustomersAccessSuccess() throws Exception {
        UserResponse customer = new UserResponse(
                1L, "Test Customer", "customer@example.com",
                "9876543210", "Delhi", Role.ROLE_CUSTOMER, true, LocalDateTime.now()
        );

        when(adminService.getAllCustomers(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer)));

        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("customer@example.com"));
    }

    @Test
    @WithMockUser(username = "admin@smartbank.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/customers/{id} - 200 OK returns customer detail with accounts")
    void testAdminCustomerDetailAccessSuccess() throws Exception {
        CustomerDetailResponse detail = new CustomerDetailResponse(
                1L, "Test Customer", "customer@example.com",
                "9876543210", "Delhi", Role.ROLE_CUSTOMER, true,
                LocalDateTime.now(), LocalDateTime.now(), List.of(), BigDecimal.ZERO
        );

        when(adminService.getCustomerById(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("customer@example.com"));
    }

    @Test
    @WithMockUser(username = "admin@smartbank.com", roles = {"ADMIN"})
    @DisplayName("PATCH /api/admin/customers/{id}/status - 200 OK updates customer status")
    void testAdminUpdateCustomerStatusSuccess() throws Exception {
        CustomerStatusUpdateRequest request = new CustomerStatusUpdateRequest(false, "Investigation hold");
        UserResponse response = new UserResponse(
                1L, "Test Customer", "customer@example.com",
                "9876543210", "Delhi", Role.ROLE_CUSTOMER, false, LocalDateTime.now()
        );

        when(adminService.updateCustomerStatus(eq(1L), any(CustomerStatusUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/admin/customers/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(username = "admin@smartbank.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/accounts - 200 OK lists accounts with filter")
    void testAdminAccountsAccessSuccess() throws Exception {
        AccountResponse acc = new AccountResponse(
                1L, "SB-1001", AccountType.SAVINGS, new BigDecimal("5000.00"),
                AccountStatus.ACTIVE, LocalDateTime.now(), "Owner", "owner@example.com"
        );

        when(adminService.getAllAccounts(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(acc)));

        mockMvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountNumber").value("SB-1001"));
    }

    @Test
    @WithMockUser(username = "admin@smartbank.com", roles = {"ADMIN"})
    @DisplayName("PATCH /api/admin/accounts/{id}/status - 200 OK updates account status")
    void testAdminUpdateAccountStatusSuccess() throws Exception {
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest(AccountStatus.FROZEN, "Compliance hold");
        AccountResponse acc = new AccountResponse(
                1L, "SB-1001", AccountType.SAVINGS, new BigDecimal("5000.00"),
                AccountStatus.FROZEN, LocalDateTime.now(), "Owner", "owner@example.com"
        );

        when(adminService.updateAccountStatus(eq(1L), any(AccountStatusUpdateRequest.class)))
                .thenReturn(acc);

        mockMvc.perform(patch("/api/admin/accounts/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("FROZEN"));
    }

    @Test
    @WithMockUser(username = "admin@smartbank.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/transactions - 200 OK lists system-wide transactions")
    void testAdminTransactionsAccessSuccess() throws Exception {
        TransactionResponse tx = new TransactionResponse(
                1L, "TXN-01", "SB-1001", null,
                TransactionType.DEPOSIT, new BigDecimal("1000.00"),
                TransactionStatus.SUCCESS, "Deposit", LocalDateTime.now()
        );

        when(adminService.getAllTransactions(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        mockMvc.perform(get("/api/admin/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionReference").value("TXN-01"));
    }

    // Role-based Access Control Tests (CUSTOMER role attempting ADMIN endpoints)

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/admin/dashboard - 403 Forbidden when CUSTOMER attempts admin dashboard")
    void testCustomerAttemptingAdminDashboardForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/admin/customers - 403 Forbidden when CUSTOMER attempts customer listing")
    void testCustomerAttemptingAdminCustomersForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("PATCH /api/admin/accounts/1/status - 403 Forbidden when CUSTOMER attempts account status modification")
    void testCustomerAttemptingAdminAccountStatusForbidden() throws Exception {
        AccountStatusUpdateRequest request = new AccountStatusUpdateRequest(AccountStatus.FROZEN, "Hold");

        mockMvc.perform(patch("/api/admin/accounts/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/admin/transactions - 403 Forbidden when CUSTOMER attempts system transaction monitoring")
    void testCustomerAttemptingAdminTransactionsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/transactions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/dashboard - 401 Unauthorized when unauthenticated")
    void testUnauthenticatedAdminAccessUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}
