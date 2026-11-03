package com.iftikar.outlier.integration.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.InvalidProtocolBufferException;
import com.iftikar.outlier.integration.google.proto.SearchUrlsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class GoogleSafeBrowsingClient {

    private static final String BASE_URL =
            "https://safebrowsing.googleapis.com";

    private static final String SEARCH_ENDPOINT =
            "/v5/urls:search";

    private final RestClient restClient;
    private final String apiKey;

    private final ConcurrentMap<String, CachedThreatResult> cache =
            new ConcurrentHashMap<>();

    public GoogleSafeBrowsingClient(
            @Value("${google.safe-browsing.api-key}") String apiKey
    ) {
        this.apiKey = apiKey;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isThreatListed(String url) {

        Instant now = Instant.now();

        CachedThreatResult cachedResult = cache.get(url);

        if (cachedResult != null) {

            if (cachedResult.expiresAt().isAfter(now)) {
                return cachedResult.threatFound();
            }

            cache.remove(url, cachedResult);
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Google Safe Browsing API key is not configured."
            );
        }

        byte[] responseBody = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_ENDPOINT)
                        .queryParam("key", apiKey)
                        .queryParam("urls", url)
                        .build()
                )
                .accept(MediaType.parseMediaType("application/x-protobuf"))
                .retrieve()
                .body(byte[].class);

        if (responseBody == null || responseBody.length == 0) {
            throw new IllegalStateException(
                    "Google Safe Browsing returned an empty response."
            );
        }

        SearchUrlsResponse response;

        try {
            response = SearchUrlsResponse.parseFrom(responseBody);
        } catch (InvalidProtocolBufferException exception) {
            throw new IllegalStateException(
                    "Could not parse Google Safe Browsing response.",
                    exception
            );
        }

        boolean threatFound = response.getThreatsCount() > 0;

        if (response.hasCacheDuration()) {

            var protobufDuration = response.getCacheDuration();

            Duration cacheDuration = Duration.ofSeconds(
                    protobufDuration.getSeconds(),
                    protobufDuration.getNanos()
            );

            if (!cacheDuration.isNegative() && !cacheDuration.isZero()) {

                Instant expiresAt = Instant.now().plus(cacheDuration);

                cache.put(
                        url,
                        new CachedThreatResult(threatFound, expiresAt)
                );
            }
        }

        removeExpiredCacheEntries();

        return threatFound;
    }

    private void removeExpiredCacheEntries() {

        Instant now = Instant.now();

        cache.entrySet().removeIf(
                entry -> !entry.getValue().expiresAt().isAfter(now)
        );
    }

    private record CachedThreatResult(
            boolean threatFound,
            Instant expiresAt
    ) {}
}
