package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.GithubRepositoryCheckResponse;
import com.iftikar.outlier.dto.LiveUrlCheckResponse;

public interface UrlValidationService {
    GithubRepositoryCheckResponse checkRepository(String url);
    LiveUrlCheckResponse checkLiveUrl(String url);
}
