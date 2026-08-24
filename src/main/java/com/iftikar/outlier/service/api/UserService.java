package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.*;

public interface UserService {
    RegisterResponse register(RegisterRequestDto request);
    boolean userExists(String username);
    String generateAccessToken(String refreshToken);
    AuthResponse login(LoginRequestDto request);
    DrawerUserInfoDto getDrawerUserInfo(String userId);
}
