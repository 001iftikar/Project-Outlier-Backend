package com.iftikar.outlier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid username address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(
                min = 6,
                message = "Password must contain at least 6 characters"
        )
        String password,

        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Role is required")
        String role
) {
}
