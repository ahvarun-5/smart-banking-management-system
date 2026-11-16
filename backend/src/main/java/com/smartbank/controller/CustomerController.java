package com.smartbank.controller;

import com.smartbank.dto.ApiResponse;
import com.smartbank.dto.ChangePasswordRequest;
import com.smartbank.dto.CustomerProfileUpdateRequest;
import com.smartbank.dto.UserResponse;
import com.smartbank.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        UserResponse profile = customerService.getProfile(authentication.getName());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody CustomerProfileUpdateRequest request,
                                                      Authentication authentication) {
        UserResponse updatedProfile = customerService.updateProfile(request, authentication.getName());
        return ResponseEntity.ok(updatedProfile);
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                      Authentication authentication) {
        customerService.changePassword(request, authentication.getName());
        return ResponseEntity.ok(new ApiResponse(true, "Password updated successfully"));
    }
}
