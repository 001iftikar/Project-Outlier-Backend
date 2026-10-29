package com.iftikar.outlier.controller;

import com.iftikar.outlier.dto.GithubRepositoryCheckRequest;
import com.iftikar.outlier.dto.GithubRepositoryCheckResponse;
import com.iftikar.outlier.result.ApiResponse;
import com.iftikar.outlier.service.api.GithubRepositoryValidationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/github")
public class GitHubRepositoryController {

    private final GithubRepositoryValidationService githubRepositoryValidationService;

    public GitHubRepositoryController(
            GithubRepositoryValidationService githubRepositoryValidationService
    ) {
        this.githubRepositoryValidationService = githubRepositoryValidationService;
    }

    @PostMapping("/validate-repository")
    public ResponseEntity<ApiResponse<GithubRepositoryCheckResponse>> validateRepository(
            @Valid @RequestBody GithubRepositoryCheckRequest request
    ) {
        GithubRepositoryCheckResponse result =
                githubRepositoryValidationService.checkRepository(request.url());

        ApiResponse<GithubRepositoryCheckResponse> response = switch (result.status()) {
            case VALID -> new ApiResponse<>(
                    "SUCCESS",
                    "Public GitHub repository verified.",
                    result
            );

            case NOT_FOUND_OR_PRIVATE -> new ApiResponse<>(
                    "GITHUB_REPOSITORY_NOT_FOUND",
                    "The repository could not be found or is not publicly accessible.",
                    result
            );

            case REPOSITORY_MOVED -> new ApiResponse<>(
                    "GITHUB_REPOSITORY_MOVED",
                    "This repository has moved. Please provide its current URL.",
                    result
            );
        };
        return ResponseEntity.ok(response);
    }
}
