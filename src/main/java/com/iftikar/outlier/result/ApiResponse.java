package com.iftikar.outlier.result;

public record ApiResponse<T>(
        String code,
        String message,
        T data
) {
}
