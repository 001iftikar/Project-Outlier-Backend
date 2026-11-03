package com.iftikar.outlier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LiveUrlCheckRequest(
        @NotBlank(message = "URL is required.")
        @Size(max = 2048, message = "URL is too long.")
        String url
) {
}
