package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.AuthResponse;
import com.iftikar.outlier.entity.EmailVerification;
import com.iftikar.outlier.entity.User;
import com.iftikar.outlier.repository.EmailVerificationRepository;
import com.iftikar.outlier.repository.UserRepository;
import com.iftikar.outlier.result.ApiException;
import com.iftikar.outlier.security.JwtService;
import com.iftikar.outlier.service.api.EmailService;
import com.iftikar.outlier.service.api.EmailVerificationService;
import com.iftikar.outlier.service.api.OtpService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private final EmailVerificationRepository verificationRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public EmailVerificationServiceImpl(
            EmailVerificationRepository verificationRepository,
            OtpService otpService,
            EmailService emailService,
            PasswordEncoder passwordEncoder, JwtService jwtService, UserRepository userRepository
    ) {
        this.verificationRepository = verificationRepository;
        this.otpService = otpService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void createVerification(String email, String username, String passwordHash, String name, String role) {
        String otp = otpService.generateOtp();

        String otpHash = passwordEncoder.encode(otp);

        Instant now = Instant.now();

        EmailVerification verification =
                EmailVerification.builder()
                        .email(email)
                        .username(username)
                        .passwordHash(passwordHash)
                        .name(name)
                        .role(role)
                        .otpHash(otpHash)
                        .expiresAt(
                                now.plus(5, ChronoUnit.MINUTES)
                        )
                        .attempts(0)
                        .createdAt(now)
                        .build();

        verificationRepository.deleteByEmail(email);

        verificationRepository.save(verification);

        emailService.sendVerificationOtp(
                email,
                otp
        );
    }

    @Transactional
    @Override
    public AuthResponse verifyOtp(String email, String otp) {
        EmailVerification verification =
                verificationRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ApiException(
                                        "VERIFICATION_NOT_FOUND",
                                        "No pending email verification found",
                                        HttpStatus.NOT_FOUND
                                )
                        );
        if (verification.getAttempts() >= 5) {

            verificationRepository.delete(verification);

            throw new ApiException(
                    "OTP_ATTEMPTS_EXCEEDED",
                    "Too many incorrect attempts. Please request a new code.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        if (verification.getExpiresAt().isBefore(Instant.now())) {

            verificationRepository.delete(verification);

            throw new ApiException(
                    "OTP_EXPIRED",
                    "Verification code has expired. Please request a new code.",
                    HttpStatus.BAD_REQUEST
            );
        }

        boolean validOtp =
                passwordEncoder.matches(
                        otp,
                        verification.getOtpHash()
                );

        if (!validOtp) {

            verification.setAttempts(
                    verification.getAttempts() + 1
            );

            verificationRepository.save(verification);

            throw new ApiException(
                    "INVALID_OTP",
                    "Invalid verification code.",
                    HttpStatus.BAD_REQUEST
            );
        }

        User user = User.builder()
                .username(verification.getUsername())
                .email(verification.getEmail())
                .passwordHash(verification.getPasswordHash())
                .name(verification.getName())
                .role(verification.getRole())
                .build();

        User savedUser = userRepository.save(user);

        verificationRepository.delete(verification);

        String accessToken =
                jwtService.generateToken(
                        savedUser.getId(),
                        savedUser.getUsername(),
                        savedUser.getRole(),
                        true
                );

        String refreshToken =
                jwtService.generateToken(
                        savedUser.getId(),
                        savedUser.getUsername(),
                        savedUser.getRole(),
                        false
                );

        return new AuthResponse(
                accessToken,
                refreshToken
        );
    }
}
















