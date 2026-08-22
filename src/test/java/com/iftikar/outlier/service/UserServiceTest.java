package com.iftikar.outlier.service;

import com.iftikar.outlier.dto.AuthResponse;
import com.iftikar.outlier.dto.RegisterRequestDto;
import com.iftikar.outlier.entity.User;
import com.iftikar.outlier.repository.UserRepository;
import com.iftikar.outlier.security.JwtService;

import com.iftikar.outlier.service.api.UserService;
import com.iftikar.outlier.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_shouldCreateUserSuccessfully() {

        // Arrange
        RegisterRequestDto request = new RegisterRequestDto(
                "ryu",
                "ryu@example.com",
                "password123",
                "Ryu",
                "DEVELOPER"
        );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
                .thenReturn("hashed-password");

        User savedUser = User.builder()
                .id("123")
                .username("ryu")
                .email("ryu@example.com")
                .passwordHash("hashed-password")
                .name("Ryu")
                .role("DEVELOPER")
                .build();

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        when(jwtService.generateToken(
                "123",
                "ryu",
                "DEVELOPER",
                true
        )).thenReturn("access-token");

        when(jwtService.generateToken(
                "123",
                "ryu",
                "DEVELOPER",
                false
        )).thenReturn("refresh-token");

        // Act
        AuthResponse result = userService.register(request);

        // Assert
        assertNotNull(result);

        assertEquals(
                "access-token",
                result.accessToken()
        );

        assertEquals(
                "refresh-token",
                result.refreshToken()
        );
    }
}