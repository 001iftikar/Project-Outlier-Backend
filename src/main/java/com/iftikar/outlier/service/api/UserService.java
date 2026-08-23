package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.AuthResponse;
import com.iftikar.outlier.dto.RegisterRequestDto;
import com.iftikar.outlier.dto.RegisterResponse;
import com.iftikar.outlier.entity.User;

import java.util.Optional;

public interface UserService {
    RegisterResponse register(RegisterRequestDto request);
    boolean userExists(String username);
    String generateAccessToken(String refreshToken);
}
