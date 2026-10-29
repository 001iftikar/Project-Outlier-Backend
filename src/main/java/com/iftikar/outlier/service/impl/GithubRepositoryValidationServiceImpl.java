package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.GithubRepositoryCheckResponse;
import com.iftikar.outlier.result.ApiException;
import com.iftikar.outlier.service.api.GithubRepositoryValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;


@Service
public class GithubRepositoryValidationServiceImpl implements GithubRepositoryValidationService {

    private static final Logger log =
            LoggerFactory.getLogger(GithubRepositoryValidationServiceImpl.class);

    private final RestClient githubApi;

    public GithubRepositoryValidationServiceImpl() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.githubApi = RestClient.builder()
                .baseUrl("https://api.github.com")
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        "application/vnd.github+json"
                )
                .defaultHeader(
                        "X-GitHub-Api-Version",
                        "2026-03-10"
                )
                .defaultHeader(
                        "User-Agent",
                        "Outlier"
                )
                .build();
    }

    @Override
    public GithubRepositoryCheckResponse checkRepository(String url) {

        // 1. Validate the URL locally before calling GitHub.
        ParsedRepository repository = parseRepositoryUrl(url);

        try {
            // 2. Call GitHub and capture the response for debugging.
            GitHubApiResponse apiResponse = githubApi.get()
                    .uri(
                            "/repos/{owner}/{repository}",
                            repository.owner(),
                            repository.repository()
                    )
                    .exchange((request, response) -> {

                        int status = response.getStatusCode().value();

                        String body = "";

                        // Read the response body for non-success responses.
                        if (status != 200) {
                            body = new String(
                                    response.getBody().readAllBytes(),
                                    StandardCharsets.UTF_8
                            );
                        }

                        String remaining = response.getHeaders()
                                .getFirst("X-RateLimit-Remaining");

                        String reset = response.getHeaders()
                                .getFirst("X-RateLimit-Reset");

                        String retryAfter = response.getHeaders()
                                .getFirst("Retry-After");

                        String requestId = response.getHeaders()
                                .getFirst("X-GitHub-Request-Id");

                        if (status != 200 && status != 404) {
                            log.warn(
                                    "GitHub API response: status={}, "
                                            + "repository={}, remaining={}, "
                                            + "reset={}, retryAfter={}, "
                                            + "requestId={}, body={}",
                                    status,
                                    repository.normalizedUrl(),
                                    remaining,
                                    reset,
                                    retryAfter,
                                    requestId,
                                    body
                            );
                        }

                        return new GitHubApiResponse(
                                status,
                                body
                        );
                    });

            int httpStatus = apiResponse.status();

            return switch (httpStatus) {

                case 200 -> new GithubRepositoryCheckResponse(
                        GithubRepositoryCheckResponse.Status.VALID,
                        true,
                        repository.normalizedUrl()
                );

                case 301, 302, 307, 308 ->
                        new GithubRepositoryCheckResponse(
                                GithubRepositoryCheckResponse.Status.REPOSITORY_MOVED,
                                false,
                                repository.normalizedUrl()
                        );

                case 404 -> new GithubRepositoryCheckResponse(
                        GithubRepositoryCheckResponse.Status.NOT_FOUND_OR_PRIVATE,
                        false,
                        repository.normalizedUrl()
                );

                case 401, 403, 429 -> throw new ApiException(
                        "GITHUB_API_UNAVAILABLE",
                        "GitHub could not verify the repository right now. "
                                + "Try again later.",
                        HttpStatus.SERVICE_UNAVAILABLE
                );

                default -> throw new ApiException(
                        "GITHUB_API_UNAVAILABLE",
                        "GitHub could not verify the repository right now. "
                                + "Try again later.",
                        HttpStatus.SERVICE_UNAVAILABLE
                );
            };

        } catch (RestClientException exception) {

            log.error(
                    "Failed to communicate with GitHub while checking repository {}",
                    repository.normalizedUrl(),
                    exception
            );

            throw new ApiException(
                    "GITHUB_API_UNAVAILABLE",
                    "Could not reach GitHub to verify the repository.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    private ParsedRepository parseRepositoryUrl(String rawUrl) {

        if (rawUrl == null
                || rawUrl.isBlank()
                || rawUrl.length() > 2048) {
            throw invalidGitHubUrl();
        }

        URI uri;

        try {
            uri = URI.create(rawUrl.strip());
        } catch (IllegalArgumentException exception) {
            throw invalidGitHubUrl();
        }

        // Require HTTPS, the exact hostname, and no unexpected components.
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"github.com".equalsIgnoreCase(uri.getHost())
                || uri.getRawUserInfo() != null
                || uri.getPort() != -1
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw invalidGitHubUrl();
        }

        String path = uri.getRawPath();

        if (path == null
                || !path.startsWith("/")
                || path.contains("%")
                || path.contains("\\")) {
            throw invalidGitHubUrl();
        }

        // Remove the initial slash.
        path = path.substring(1);

        // Permit one trailing slash.
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        String[] parts = path.split("/", -1);

        // The accepted format is /owner/repository.
        if (parts.length != 2
                || !isValidPathSegment(parts[0])
                || !isValidPathSegment(parts[1])
                || parts[0].equals(".")
                || parts[0].equals("..")
                || parts[1].equals(".")
                || parts[1].equals("..")) {
            throw invalidGitHubUrl();
        }

        String owner = parts[0];
        String repository = parts[1];

        // Accept repository URLs ending with .git.
        if (repository.toLowerCase(Locale.ROOT).endsWith(".git")) {
            repository = repository.substring(
                    0,
                    repository.length() - 4
            );
        }

        if (repository.isBlank()
                || repository.equals(".")
                || repository.equals("..")) {
            throw invalidGitHubUrl();
        }

        String normalizedUrl =
                "https://github.com/" + owner + "/" + repository;

        return new ParsedRepository(
                owner,
                repository,
                normalizedUrl
        );
    }

    private boolean isValidPathSegment(String segment) {
        return segment != null
                && segment.matches("[A-Za-z0-9._-]{1,200}");
    }

    private ApiException invalidGitHubUrl() {
        return new ApiException(
                "INVALID_GITHUB_URL",
                "Enter a valid GitHub repository URL, such as "
                        + "https://github.com/owner/repository.",
                HttpStatus.BAD_REQUEST
        );
    }

    private record ParsedRepository(
            String owner,
            String repository,
            String normalizedUrl
    ) {
    }

    private record GitHubApiResponse(
            int status,
            String body
    ) {
    }
}
