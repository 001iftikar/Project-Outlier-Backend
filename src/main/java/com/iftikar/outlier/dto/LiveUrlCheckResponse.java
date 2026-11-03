package com.iftikar.outlier.dto;

public record LiveUrlCheckResponse(
        Status status,
        boolean safe
) {

    public enum Status {
        SAFE,
        UNSAFE,
        UNVERIFIED
    }
}
