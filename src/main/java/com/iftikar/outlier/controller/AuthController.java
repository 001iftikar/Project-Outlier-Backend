package com.iftikar.outlier.controller;

import com.iftikar.outlier.dto.AuthResponse;
import com.iftikar.outlier.dto.RegisterRequestDto;
import com.iftikar.outlier.dto.RegisterResponse;
import com.iftikar.outlier.dto.VerifyEmailRequestDto;
import com.iftikar.outlier.result.ApiResponse;
import com.iftikar.outlier.service.api.EmailVerificationService;
import com.iftikar.outlier.service.api.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(UserService userService, EmailVerificationService emailVerificationService) {
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequestDto request
    ) {
        RegisterResponse result = userService.register(request);
        ApiResponse<RegisterResponse> response =
                new ApiResponse<>(
                        "OTP_SENT",
                        "Verification code sent to email",
                        result
                );
        return ResponseEntity
                .ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequestDto request
    ) {

        AuthResponse authResponse =
                emailVerificationService.verifyOtp(
                        request.email(),
                        request.otp()
                );

        ApiResponse<AuthResponse> response =
                new ApiResponse<>(
                        "REGISTRATION_SUCCESS",
                        "Account created successfully",
                        authResponse
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/refresh-token/{userId}")
    public ResponseEntity<ApiResponse<String>> getRefreshToken(
            @PathVariable String userId
    ) {
        String accessToken = userService.generateAccessToken(userId);
        ApiResponse<String> response = new ApiResponse<>(
                "TOKEN_GENERATIONN_SUCCESS",
                "Access token is returned successfully",
                accessToken
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
