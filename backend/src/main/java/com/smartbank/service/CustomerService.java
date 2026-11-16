package com.smartbank.service;

import com.smartbank.dto.ChangePasswordRequest;
import com.smartbank.dto.CustomerProfileUpdateRequest;
import com.smartbank.dto.UserResponse;

public interface CustomerService {

    UserResponse getProfile(String userEmail);

    UserResponse updateProfile(CustomerProfileUpdateRequest request, String userEmail);

    void changePassword(ChangePasswordRequest request, String userEmail);
}
