package com.smartbank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbank.dto.ChangePasswordRequest;
import com.smartbank.dto.CustomerProfileUpdateRequest;
import com.smartbank.dto.UserResponse;
import com.smartbank.entity.Role;
import com.smartbank.exception.InvalidCredentialsException;
import com.smartbank.service.CustomerService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("GET /api/customers/profile - 200 OK returns customer profile")
    void testGetProfileSuccess() throws Exception {
        UserResponse response = new UserResponse(
                1L, "Test Customer", "customer@smartbank.com",
                "9876543210", "123 Main St", Role.ROLE_CUSTOMER,
                true, LocalDateTime.now()
        );

        when(customerService.getProfile("customer@smartbank.com")).thenReturn(response);

        mockMvc.perform(get("/api/customers/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@smartbank.com"))
                .andExpect(jsonPath("$.fullName").value("Test Customer"));
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("PUT /api/customers/profile - 200 OK on valid update")
    void testUpdateProfileSuccess() throws Exception {
        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest("Updated Customer", "9988776655", "456 Oak Avenue");
        UserResponse response = new UserResponse(
                1L, "Updated Customer", "customer@smartbank.com",
                "9988776655", "456 Oak Avenue", Role.ROLE_CUSTOMER,
                true, LocalDateTime.now()
        );

        when(customerService.updateProfile(any(CustomerProfileUpdateRequest.class), eq("customer@smartbank.com")))
                .thenReturn(response);

        mockMvc.perform(put("/api/customers/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Customer"))
                .andExpect(jsonPath("$.phoneNumber").value("9988776655"));
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("PUT /api/customers/profile - 400 Bad Request on blank full name")
    void testUpdateProfileValidationFailure() throws Exception {
        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest("", "9988776655", "456 Oak Avenue");

        mockMvc.perform(put("/api/customers/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("PUT /api/customers/change-password - 200 OK on successful password change")
    void testChangePasswordSuccess() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "NewPass@456", "NewPass@456");

        doNothing().when(customerService).changePassword(any(ChangePasswordRequest.class), eq("customer@smartbank.com"));

        mockMvc.perform(put("/api/customers/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password updated successfully"));
    }

    @Test
    @WithMockUser(username = "customer@smartbank.com", roles = {"CUSTOMER"})
    @DisplayName("PUT /api/customers/change-password - 401 Unauthorized when current password wrong")
    void testChangePasswordWrongCurrent() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPass@123", "NewPass@456", "NewPass@456");

        doThrow(new InvalidCredentialsException("Current password is incorrect"))
                .when(customerService).changePassword(any(ChangePasswordRequest.class), eq("customer@smartbank.com"));

        mockMvc.perform(put("/api/customers/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/customers/profile - 401 Unauthorized when unauthenticated")
    void testGetProfileUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/customers/profile"))
                .andExpect(status().isUnauthorized());
    }
}
