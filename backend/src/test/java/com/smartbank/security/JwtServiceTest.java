package com.smartbank.security;

import com.smartbank.entity.Role;
import com.smartbank.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set standard 256-bit test secret and 1 hour expiration
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);

        User user = new User("Alice Test", "alice@smartbank.com", "9998887776", "pass", "Address", Role.ROLE_CUSTOMER);
        user.setId(5L);
        userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("Should generate valid token and extract username and claims")
    void testGenerateAndValidateToken() {
        String token = jwtService.generateToken(userDetails, 5L, "ROLE_CUSTOMER");

        assertThat(token).isNotBlank();

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("alice@smartbank.com");

        boolean isValid = jwtService.validateToken(token, userDetails);
        assertThat(isValid).isTrue();

        boolean isExpired = jwtService.isTokenExpired(token);
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should reject token for non-matching user")
    void testTokenValidationDifferentUser() {
        String token = jwtService.generateToken(userDetails, 5L, "ROLE_CUSTOMER");

        User otherUser = new User("Bob Test", "bob@smartbank.com", "9998887775", "pass", "Address", Role.ROLE_CUSTOMER);
        CustomUserDetails otherUserDetails = new CustomUserDetails(otherUser);

        boolean isValid = jwtService.validateToken(token, otherUserDetails);
        assertThat(isValid).isFalse();
    }
}
