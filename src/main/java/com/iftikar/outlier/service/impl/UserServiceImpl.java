package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.RegisterRequestDto;
import com.iftikar.outlier.dto.RegisterResponse;
import com.iftikar.outlier.repository.UserRepository;
import com.iftikar.outlier.result.ApiException;
import com.iftikar.outlier.service.api.EmailVerificationService;
import com.iftikar.outlier.service.api.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    public RegisterResponse register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.email())) {

            throw new ApiException(
                    "EMAIL_ALREADY_EXISTS",
                    "Email already exists",
                    HttpStatus.CONFLICT
            );
        }

        String passwordHash = passwordEncoder.encode(request.password());
        emailVerificationService.createVerification(
                request.email(),
                request.username(),
                passwordHash,
                request.name(),
                request.role()
        );

        return new RegisterResponse(request.email());
    }

    @Override
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }
}














