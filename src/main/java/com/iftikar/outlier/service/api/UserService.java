package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.AuthResponse;
import com.iftikar.outlier.dto.RegisterRequestDto;

public interface UserService {
    public AuthResponse register(RegisterRequestDto request);
    public boolean userExists(String username);
}
