package com.smartbank.service.impl;

import com.smartbank.dto.ChangePasswordRequest;
import com.smartbank.dto.CustomerProfileUpdateRequest;
import com.smartbank.dto.UserResponse;
import com.smartbank.entity.User;
import com.smartbank.exception.InvalidCredentialsException;
import com.smartbank.exception.InvalidTransactionException;
import com.smartbank.exception.ResourceNotFoundException;
import com.smartbank.repository.UserRepository;
import com.smartbank.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(String userEmail) {
        User user = getUserByEmail(userEmail);
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(CustomerProfileUpdateRequest request, String userEmail) {
        log.info("Updating profile for customer: {}", userEmail);

        User user = getUserByEmail(userEmail);

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setAddress(request.getAddress().trim());

        User updatedUser = userRepository.save(user);
        log.info("Profile successfully updated for customer: {}", userEmail);

        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, String userEmail) {
        log.info("Processing password change for user: {}", userEmail);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidTransactionException("New password and confirm password do not match");
        }

        User user = getUserByEmail(userEmail);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change failed: incorrect current password for user {}", userEmail);
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidTransactionException("New password cannot be the same as the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password successfully changed for user: {}", userEmail);
    }

    private User getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        if (!user.isEnabled()) {
            throw new com.smartbank.exception.UnauthorizedAccessException("User account is deactivated.");
        }
        return user;
    }
}
