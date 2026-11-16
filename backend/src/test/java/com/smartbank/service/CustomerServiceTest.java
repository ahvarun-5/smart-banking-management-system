package com.smartbank.service;

import com.smartbank.dto.ChangePasswordRequest;
import com.smartbank.dto.CustomerProfileUpdateRequest;
import com.smartbank.dto.UserResponse;
import com.smartbank.entity.Role;
import com.smartbank.entity.User;
import com.smartbank.exception.InvalidCredentialsException;
import com.smartbank.exception.InvalidTransactionException;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.repository.UserRepository;
import com.smartbank.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("Vikram Singh", "vikram@example.com", "9899887766", "encodedOldPassword", "Jaipur", Role.ROLE_CUSTOMER);
        testUser.setId(10L);
    }

    @Test
    @DisplayName("Get Profile - Returns user profile details")
    void testGetProfileSuccess() {
        when(userRepository.findByEmail("vikram@example.com")).thenReturn(Optional.of(testUser));

        UserResponse response = customerService.getProfile("vikram@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("vikram@example.com");
        assertThat(response.getFullName()).isEqualTo("Vikram Singh");
        assertThat(response.getPhoneNumber()).isEqualTo("9899887766");
    }

    @Test
    @DisplayName("Update Profile - Updates name, phone, and address")
    void testUpdateProfileSuccess() {
        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest("Vikram S. Rathore", "9111223344", "Udaipur, Rajasthan");
        when(userRepository.findByEmail("vikram@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = customerService.updateProfile(request, "vikram@example.com");

        assertThat(response).isNotNull();
        assertThat(testUser.getFullName()).isEqualTo("Vikram S. Rathore");
        assertThat(testUser.getPhoneNumber()).isEqualTo("9111223344");
        assertThat(testUser.getAddress()).isEqualTo("Udaipur, Rajasthan");
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Change Password - Successful password update encodes new password")
    void testChangePasswordSuccess() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "NewPass@456", "NewPass@456");
        when(userRepository.findByEmail("vikram@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPass@123", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPass@456", "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("encodedNewPassword");

        customerService.changePassword(request, "vikram@example.com");

        assertThat(testUser.getPassword()).isEqualTo("encodedNewPassword");
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Change Password - Wrong current password throws InvalidCredentialsException")
    void testChangePasswordWrongCurrentPasswordThrows() {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPass@123", "NewPass@456", "NewPass@456");
        when(userRepository.findByEmail("vikram@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPass@123", "encodedOldPassword")).thenReturn(false);

        assertThatThrownBy(() -> customerService.changePassword(request, "vikram@example.com"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Change Password - Confirm password mismatch throws InvalidTransactionException")
    void testChangePasswordMismatchConfirmPasswordThrows() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "NewPass@456", "DifferentPass@789");

        assertThatThrownBy(() -> customerService.changePassword(request, "vikram@example.com"))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("do not match");

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Change Password - New password same as current password throws InvalidTransactionException")
    void testChangePasswordSameAsOldThrows() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "OldPass@123", "OldPass@123");
        when(userRepository.findByEmail("vikram@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPass@123", "encodedOldPassword")).thenReturn(true);

        assertThatThrownBy(() -> customerService.changePassword(request, "vikram@example.com"))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("same as the current password");

        verify(userRepository, never()).save(any());
    }
}
