package com.iftikar.outlier.service.api;

public interface EmailService {
    public void sendVerificationOtp(String recipientEmail, String otp);
}
