package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.GithubRepositoryCheckResponse;

public interface GithubRepositoryValidationService {
    GithubRepositoryCheckResponse checkRepository(String url);
}
