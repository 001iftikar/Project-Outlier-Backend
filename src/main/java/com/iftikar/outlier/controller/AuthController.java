package com.iftikar.outlier.controller;

import com.iftikar.outlier.dto.*;
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

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<String>> refreshToken(
            @RequestBody RefreshTokenRequest request
    ) {

        String accessToken =
                userService.generateAccessToken(
                        request.refreshToken()
                );

        ApiResponse<String> response =
                new ApiResponse<>(
                        "TOKEN_GENERATION_SUCCESS",
                        "Access token is returned successfully",
                        accessToken
                );

        return ResponseEntity.ok(response);
    }

    /**
     * To check if username is available while typing it
     */
    @GetMapping("/check-username/{username}")
    ResponseEntity<ApiResponse<Boolean>> checkUsernameAvailability(
            @PathVariable String username
    ) {
        boolean userExists = userService.userExists(username);
        ApiResponse<Boolean> response = new ApiResponse<>(
                "CHECK_SUCCESS",
                "api returned if username is available",
                userExists
        );
        return ResponseEntity
                .ok(response);
    }
}
