package com.iftikar.outlier.dto;

public record PostLikeResponse(
        boolean liked,
        long likesCount
) {}