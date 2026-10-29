package com.iftikar.outlier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GithubRepositoryCheckRequest(
        @NotBlank(message = "GitHub repository URL is required.")
        @Size(max = 2048, message = "GitHub URL is too long.")
        String url
) {
}
