package com.iftikar.outlier.dto;

public record GithubRepositoryCheckResponse(
        Status status,
        boolean valid,
        String normalizedUrl
) {

    public enum Status {
        VALID,
        NOT_FOUND_OR_PRIVATE,
        REPOSITORY_MOVED
    }
}