package com.iftikar.outlier.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
