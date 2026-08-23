package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.AuthResponse;
import com.iftikar.outlier.dto.RegisterRequestDto;
import com.iftikar.outlier.dto.RegisterResponse;

public interface UserService {
    public RegisterResponse register(RegisterRequestDto request);
    public boolean userExists(String username);
}
