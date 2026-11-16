package com.smartbank.service;

import com.smartbank.dto.AuthResponse;
import com.smartbank.dto.LoginRequest;
import com.smartbank.dto.RegisterRequest;
import com.smartbank.dto.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String bearerToken);

    UserResponse getCurrentUser(String email);
}
