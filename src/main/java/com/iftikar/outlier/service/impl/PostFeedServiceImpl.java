package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.PostFeedItemResponse;
import com.iftikar.outlier.dto.PostFeedResponse;
import com.iftikar.outlier.repository.PostRepository;
import com.iftikar.outlier.result.ApiException;
import com.iftikar.outlier.service.api.PostFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostFeedServiceImpl implements PostFeedService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final PostResponseAssembler postResponseAssembler;

    @Override
    @Transactional(readOnly = true)
    public PostFeedResponse getFeed(
            String userId,
            String cursor,
            int size
    ) {
        validatePageSize(size);

        FeedCursor decodedCursor = decodeCursor(cursor);
        int fetchSize = size + 1;

        List<PostRepository.FeedCursorProjection> fetchedRows;

        if (decodedCursor == null) {
            fetchedRows = postRepository.findFirstFeedWindow(
                    PageRequest.of(0, fetchSize)
            );
        } else {
            fetchedRows = postRepository.findFeedWindowAfter(
                    decodedCursor.createdAt(),
                    decodedCursor.postId(),
                    PageRequest.of(0, fetchSize)
            );
        }

        boolean hasMore = fetchedRows.size() > size;

        List<PostRepository.FeedCursorProjection> pageRows = hasMore
                ? fetchedRows.subList(0, size)
                : fetchedRows;

        if (pageRows.isEmpty()) {
            return new PostFeedResponse(List.of(), null, false);
        }

        List<String> postIds = pageRows.stream()
                .map(PostRepository.FeedCursorProjection::getPostId)
                .toList();

        List<PostFeedItemResponse> posts =
                postResponseAssembler.assemble(userId, postIds);

        String nextCursor = null;

        if (hasMore) {
            var lastRow = pageRows.get(pageRows.size() - 1);
            nextCursor = encodeCursor(
                    lastRow.getCreatedAt(),
                    lastRow.getPostId()
            );
        }

        return new PostFeedResponse(posts, nextCursor, hasMore);
    }

    private void validatePageSize(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    "INVALID_PAGE_SIZE",
                    "Page size must be between 1 and 50.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String encodeCursor(Instant createdAt, String postId) {
        String rawCursor = createdAt + "|" + postId;

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
    }

    private FeedCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        if (cursor.length() > 512) {
            throw invalidCursor();
        }

        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cursor);
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            int separator = decoded.indexOf('|');

            if (separator <= 0 || separator == decoded.length() - 1) {
                throw invalidCursor();
            }

            Instant createdAt = Instant.parse(decoded.substring(0, separator));
            String postId = decoded.substring(separator + 1);

            if (postId.isBlank() || postId.length() > 64) {
                throw invalidCursor();
            }

            return new FeedCursor(createdAt, postId);
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw invalidCursor();
        }
    }

    private ApiException invalidCursor() {
        return new ApiException(
                "INVALID_CURSOR",
                "The supplied feed cursor is invalid.",
                HttpStatus.BAD_REQUEST
        );
    }

    private record FeedCursor(Instant createdAt, String postId) {}
}
