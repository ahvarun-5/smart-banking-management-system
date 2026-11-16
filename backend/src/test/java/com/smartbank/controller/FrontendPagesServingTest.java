package com.smartbank.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class FrontendPagesServingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / - Should serve root landing page")
    void testRootPageServed() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /index.html - Should serve index.html")
    void testIndexHtmlServed() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("GET /css/styles.css - Should serve CSS stylesheet")
    void testCssServed() throws Exception {
        mockMvc.perform(get("/css/styles.css"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /js/api.js - Should serve API client script")
    void testApiJsServed() throws Exception {
        mockMvc.perform(get("/js/api.js"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /js/auth.js - Should serve Auth script")
    void testAuthJsServed() throws Exception {
        mockMvc.perform(get("/js/auth.js"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /pages/login.html - Should serve login page")
    void testLoginPageServed() throws Exception {
        mockMvc.perform(get("/pages/login.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("GET /pages/register.html - Should serve registration page")
    void testRegisterPageServed() throws Exception {
        mockMvc.perform(get("/pages/register.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("GET /pages/dashboard.html - Should serve customer dashboard page")
    void testDashboardPageServed() throws Exception {
        mockMvc.perform(get("/pages/dashboard.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("GET /pages/accounts.html - Should serve accounts page")
    void testAccountsPageServed() throws Exception {
        mockMvc.perform(get("/pages/accounts.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("GET /pages/transactions.html - Should serve transactions page")
    void testTransactionsPageServed() throws Exception {
        mockMvc.perform(get("/pages/transactions.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("GET /pages/profile.html - Should serve profile page")
    void testProfilePageServed() throws Exception {
        mockMvc.perform(get("/pages/profile.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("GET /pages/admin-dashboard.html - Should serve admin dashboard page")
    void testAdminDashboardPageServed() throws Exception {
        mockMvc.perform(get("/pages/admin-dashboard.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("GET /pages/admin-customers.html - Should serve admin customers page")
    void testAdminCustomersPageServed() throws Exception {
        mockMvc.perform(get("/pages/admin-customers.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("GET /pages/admin-accounts.html - Should serve admin accounts page")
    void testAdminAccountsPageServed() throws Exception {
        mockMvc.perform(get("/pages/admin-accounts.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("GET /pages/admin-transactions.html - Should serve admin transactions page")
    void testAdminTransactionsPageServed() throws Exception {
        mockMvc.perform(get("/pages/admin-transactions.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }
}
