package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.AuthResponse;
import com.iftikar.outlier.dto.LoginRequestDto;
import com.iftikar.outlier.dto.RegisterRequestDto;
import com.iftikar.outlier.dto.RegisterResponse;
import com.iftikar.outlier.entity.User;
import com.iftikar.outlier.repository.UserRepository;
import com.iftikar.outlier.result.ApiException;
import com.iftikar.outlier.security.JwtService;
import com.iftikar.outlier.service.api.EmailVerificationService;
import com.iftikar.outlier.service.api.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final JwtService jwtService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailVerificationService emailVerificationService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.jwtService = jwtService;
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

    @Override
    public String generateAccessToken(String refreshToken) {

        Claims claims;

        try {
            claims = jwtService.getClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {

            throw new ApiException(
                    "INVALID_REFRESH_TOKEN",
                    "Invalid refresh token",
                    HttpStatus.UNAUTHORIZED
            );
        }

        if (!jwtService.isRefreshToken(claims)) {
            throw new ApiException(
                    "INVALID_TOKEN_TYPE",
                    "Provided token is not a refresh token",
                    HttpStatus.UNAUTHORIZED
            );
        }

        String userId = claims.getSubject();

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ApiException(
                                "USER_NOT_FOUND",
                                "User associated with refresh token was not found",
                                HttpStatus.NOT_FOUND
                        )
                );

        return jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                true
        );
    }

    @Override
    public AuthResponse login(LoginRequestDto request) {
        User fetchedUser = userRepository.findByUsername(request.username())
                .orElseThrow(() ->
                        new ApiException(
                                "USER_NOT_FOUND",
                                "No user by this username",
                                HttpStatus.NOT_FOUND
                        )
                );
        String hashedPassword = fetchedUser.getPasswordHash();
        String userId = fetchedUser.getId();
        String role = fetchedUser.getRole();
        boolean validPassword =
                passwordEncoder.matches(
                        request.password(),
                        hashedPassword
                );
        if (!validPassword) {
            throw new ApiException(
                    "PASSWORD_MISMATCH",
                    "Password does not match with the username",
                    HttpStatus.UNAUTHORIZED
            );
        }

        String accessToken = jwtService.generateToken(
                userId,
                request.username(),
                role,
                true
        );
        String refreshToken = jwtService.generateToken(
                userId,
                request.username(),
                role,
                false
        );
        return new AuthResponse(
                accessToken,
                refreshToken
        );
    }
}














