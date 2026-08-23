package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.AuthResponse;

public interface EmailVerificationService {
    public void createVerification(String email,
                                   String username,
                                   String passwordHash,
                                   String name,
                                   String role);
    public AuthResponse verifyOtp(String email, String otp);
}
