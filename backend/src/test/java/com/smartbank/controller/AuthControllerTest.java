package com.smartbank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbank.dto.AuthResponse;
import com.smartbank.dto.LoginRequest;
import com.smartbank.dto.RegisterRequest;
import com.smartbank.dto.UserResponse;
import com.smartbank.entity.Role;
import com.smartbank.exception.InvalidCredentialsException;
import com.smartbank.exception.UserAlreadyExistsException;
import com.smartbank.security.CustomUserDetailsService;
import com.smartbank.security.JwtService;
import com.smartbank.security.TokenBlacklistService;
import com.smartbank.service.AuthService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register - Should return 201 Created on valid registration")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Priya Sharma",
                "priya@example.com",
                "9811223344",
                "Bangalore, India",
                "Password@123",
                "Password@123"
        );

        UserResponse userResponse = new UserResponse(1L, "Priya Sharma", "priya@example.com", "9811223344", "Bangalore, India", Role.ROLE_CUSTOMER, true, LocalDateTime.now());
        AuthResponse authResponse = new AuthResponse("mock.jwt.token", 86400000L, userResponse);

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.data.user.email").value("priya@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 Bad Request when validation fails")
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "", // Blank name
                "invalid-email-format",
                "123", // Short phone
                "",
                "123", // Too short password
                "123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.fullName").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 409 Conflict when duplicate email")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Priya Sharma",
                "priya@example.com",
                "9811223344",
                "Bangalore, India",
                "Password@123",
                "Password@123"
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("An account with email 'priya@example.com' already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An account with email 'priya@example.com' already exists"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 200 OK with token on valid credentials")
    void testLoginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest("priya@example.com", "Password@123");
        UserResponse userResponse = new UserResponse(1L, "Priya Sharma", "priya@example.com", "9811223344", "Bangalore, India", Role.ROLE_CUSTOMER, true, LocalDateTime.now());
        AuthResponse authResponse = new AuthResponse("login.mock.jwt.token", 86400000L, userResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("login.mock.jwt.token"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 Unauthorized on invalid credentials")
    void testLoginFailure() throws Exception {
        LoginRequest loginRequest = new LoginRequest("priya@example.com", "WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("GET /api/auth/me - Should return 200 OK with profile for authenticated user")
    @WithMockUser(username = "priya@example.com", authorities = {"ROLE_CUSTOMER"})
    void testGetMeAuthenticated() throws Exception {
        UserResponse userResponse = new UserResponse(1L, "Priya Sharma", "priya@example.com", "9811223344", "Bangalore, India", Role.ROLE_CUSTOMER, true, LocalDateTime.now());
        when(authService.getCurrentUser("priya@example.com")).thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("priya@example.com"));
    }

    @Test
    @DisplayName("ROLE-BASED AUTHORIZATION - Anonymous access to /api/admin/ping returns 401 Unauthorized")
    void testAdminAccessUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("ROLE-BASED AUTHORIZATION - CUSTOMER role access to /api/admin/ping returns 403 Forbidden")
    @WithMockUser(username = "customer@example.com", authorities = {"ROLE_CUSTOMER"})
    void testAdminAccessForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("ROLE-BASED AUTHORIZATION - ADMIN role access to /api/admin/ping returns 200 OK")
    @WithMockUser(username = "admin@smartbank.com", authorities = {"ROLE_ADMIN"})
    void testAdminAccessAllowedForAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }
}
