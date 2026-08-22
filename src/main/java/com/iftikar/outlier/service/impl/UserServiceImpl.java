package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.AuthResponse;
import com.iftikar.outlier.dto.RegisterRequestDto;
import com.iftikar.outlier.entity.User;
import com.iftikar.outlier.repository.UserRepository;
import com.iftikar.outlier.result.ApiException;
import com.iftikar.outlier.security.JwtService;
import com.iftikar.outlier.service.api.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.email())) {

            throw new ApiException(
                    "EMAIL_ALREADY_EXISTS",
                    "Email already exists",
                    HttpStatus.CONFLICT
            );
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordHash)
                .name(request.name())
                .role(request.role())
                .build();

        User savedUser = userRepository.save(user);
        String accessToken = jwtService.generateToken(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole(),
                true
        );
        String refreshToken = jwtService.generateToken(
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

    @Override
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }
}














