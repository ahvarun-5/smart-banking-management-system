package com.smartbank.service.impl;

import com.smartbank.dto.AuthResponse;
import com.smartbank.dto.LoginRequest;
import com.smartbank.dto.RegisterRequest;
import com.smartbank.dto.UserResponse;
import com.smartbank.entity.Role;
import com.smartbank.entity.User;
import com.smartbank.exception.AccountInactiveException;
import com.smartbank.exception.InvalidCredentialsException;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.exception.UserAlreadyExistsException;
import com.smartbank.repository.UserRepository;
import com.smartbank.security.CustomUserDetails;
import com.smartbank.security.JwtService;
import com.smartbank.security.TokenBlacklistService;
import com.smartbank.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Processing customer registration request for email: {}", request.getEmail());

        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            logger.warn("Registration rejected: Email already in use: {}", normalizedEmail);
            throw new UserAlreadyExistsException("An account with email '" + normalizedEmail + "' already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setAddress(request.getAddress().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_CUSTOMER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        logger.info("Successfully registered customer with ID: {}", savedUser.getId());

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtService.generateToken(userDetails, savedUser.getId(), savedUser.getRole().name());

        return new AuthResponse(token, jwtService.getExpirationMs(), UserResponse.fromEntity(savedUser));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        logger.info("Attempting authentication for email: {}", normalizedEmail);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            if (!user.isEnabled()) {
                logger.warn("Authentication failed: User account is deactivated: {}", normalizedEmail);
                throw new AccountInactiveException("This user account is disabled. Please contact the administrator.");
            }

            String token = jwtService.generateToken(userDetails, user.getId(), user.getRole().name());
            logger.info("User authenticated successfully: {}", normalizedEmail);

            return new AuthResponse(token, jwtService.getExpirationMs(), UserResponse.fromEntity(user));

        } catch (BadCredentialsException ex) {
            logger.warn("Bad credentials for email: {}", normalizedEmail);
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Override
    public void logout(String bearerToken) {
        if (bearerToken != null && !bearerToken.isBlank()) {
            String token = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7).trim() : bearerToken.trim();
            try {
                Date expiration = jwtService.extractExpiration(token);
                tokenBlacklistService.blacklistToken(token, expiration != null ? expiration.toInstant() : Instant.now().plusSeconds(86400));
                logger.info("User successfully logged out and token invalidated");
            } catch (Exception e) {
                // If token is already malformed or expired, still add it to blacklist for security
                tokenBlacklistService.blacklistToken(token, Instant.now().plusSeconds(3600));
                logger.info("Logged out with expired/malformed token");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserResponse.fromEntity(user);
    }
}
