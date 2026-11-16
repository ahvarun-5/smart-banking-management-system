package com.smartbank.service;

import com.smartbank.dto.AuthResponse;
import com.smartbank.dto.LoginRequest;
import com.smartbank.dto.RegisterRequest;
import com.smartbank.dto.UserResponse;
import com.smartbank.entity.Role;
import com.smartbank.entity.User;
import com.smartbank.exception.AccountInactiveException;
import com.smartbank.exception.InvalidCredentialsException;
import com.smartbank.exception.UserAlreadyExistsException;
import com.smartbank.repository.UserRepository;
import com.smartbank.security.CustomUserDetails;
import com.smartbank.security.JwtService;
import com.smartbank.security.TokenBlacklistService;
import com.smartbank.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User customerUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "John Doe",
                "john.doe@example.com",
                "9876543210",
                "123 Banking Street, Financial District",
                "Secret@123",
                "Secret@123"
        );

        loginRequest = new LoginRequest("john.doe@example.com", "Secret@123");

        customerUser = new User(
                "John Doe",
                "john.doe@example.com",
                "9876543210",
                "encodedPasswordHash",
                "123 Banking Street, Financial District",
                Role.ROLE_CUSTOMER
        );
        customerUser.setId(10L);
        customerUser.setEnabled(true);
    }

    @Test
    @DisplayName("Should successfully register customer, hash password, and return JWT token")
    void testSuccessfulRegistration() {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("encodedPasswordHash");
        when(userRepository.save(any(User.class))).thenReturn(customerUser);
        when(jwtService.generateToken(any(), eq(10L), eq("ROLE_CUSTOMER"))).thenReturn("sample.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("sample.jwt.token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getUser().getRole()).isEqualTo(Role.ROLE_CUSTOMER);

        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("Secret@123");
    }

    @Test
    @DisplayName("Should reject registration when passwords do not match")
    void testRegistrationPasswordMismatch() {
        registerRequest.setConfirmPassword("Mismatch@123");

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not match");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration when email is already registered")
    void testRegistrationDuplicateEmail() {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully authenticate and return token for valid login")
    void testSuccessfulLogin() {
        CustomUserDetails userDetails = new CustomUserDetails(customerUser);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(any(), eq(10L), eq("ROLE_CUSTOMER"))).thenReturn("login.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("login.jwt.token");
        assertThat(response.getUser().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when bad credentials provided")
    void testLoginBadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should reject login when customer account is deactivated")
    void testLoginDisabledAccount() {
        customerUser.setEnabled(false);
        CustomUserDetails userDetails = new CustomUserDetails(customerUser);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("Should fetch current user profile by email")
    void testGetCurrentUser() {
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(customerUser));

        UserResponse response = authService.getCurrentUser("john.doe@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("John Doe");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
    }
}
