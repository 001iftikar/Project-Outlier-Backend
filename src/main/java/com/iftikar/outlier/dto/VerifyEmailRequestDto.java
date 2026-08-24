package com.iftikar.outlier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid username address")
        String email,

        @NotBlank(message = "OTP is required")
        String otp
) {
}
