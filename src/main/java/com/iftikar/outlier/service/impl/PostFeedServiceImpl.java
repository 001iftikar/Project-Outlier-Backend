
package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.PostAuthorResponse;
import com.iftikar.outlier.dto.PostFeedItemResponse;
import com.iftikar.outlier.dto.PostFeedResponse;
import com.iftikar.outlier.repository.*;
import com.iftikar.outlier.result.ApiException;
import com.iftikar.outlier.service.api.PostFeedService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostFeedServiceImpl implements PostFeedService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostTechnologyRepository postTechnologyRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserTechnologyRepository userTechnologyRepository;

    @Override
    @Transactional(readOnly = true)
    public PostFeedResponse getFeed(
            String userId,
            String cursor,
            int size
    ) {

        // 1. Validate page size.
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    "INVALID_PAGE_SIZE",
                    "Page size must be between 1 and 50.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 2. Decode the cursor, if supplied.
        FeedCursor decodedCursor = decodeCursor(cursor);

        // 3. Fetch one extra row to determine whether more posts exist.
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

        // 4. Determine whether another page exists.
        boolean hasMore = fetchedRows.size() > size;

        // 5. Remove the extra row.
        List<PostRepository.FeedCursorProjection> pageRows =
                hasMore
                        ? fetchedRows.subList(0, size)
                        : fetchedRows;

        if (pageRows.isEmpty()) {
            return new PostFeedResponse(
                    List.of(),
                    null,
                    false
            );
        }

        // 6. Collect post IDs in feed order.
        List<String> postIds = pageRows.stream()
                .map(PostRepository.FeedCursorProjection::getPostId)
                .toList();

        // 7. Fetch basic post and author details in one query.
        List<PostRepository.FeedPostProjection> baseRows =
                postRepository.findFeedPostsByIds(postIds);
        // Fetch all author technologies in one query.
        List<String> userIds = baseRows.stream()
                .map(PostRepository.FeedPostProjection::getAuthorId)
                .distinct()
                .toList();

        Map<String, List<String>> technologiesByUser = new HashMap<>();

        if (!userIds.isEmpty()) {

            var userTechnologyRows =
                    userTechnologyRepository.findTechnologiesByUserIds(userIds);

            for (var row : userTechnologyRows) {

                technologiesByUser
                        .computeIfAbsent(
                                row.getUserId(),
                                ignored -> new ArrayList<>()
                        )
                        .add(row.getTechnologyName());
            }
        }

        Map<String, PostRepository.FeedPostProjection> postsById =
                new HashMap<>();

        for (var row : baseRows) {
            postsById.put(row.getPostId(), row);
        }

        // 8. Fetch all images for the selected posts.
        Map<String, List<String>> imagesByPost = new HashMap<>();

        var imageRows =
                postImageRepository.findFeedImagesByPostIds(postIds);

        for (var row : imageRows) {

            imagesByPost
                    .computeIfAbsent(
                            row.getPostId(),
                            ignored -> new ArrayList<>()
                    )
                    .add(row.getImageUrl());
        }

        // 9. Fetch all technologies for the selected posts.
        Map<String, List<String>> technologiesByPost = new HashMap<>();

        var technologyRows =
                postTechnologyRepository.findFeedTechnologiesByPostIds(
                        postIds
                );

        for (var row : technologyRows) {

            technologiesByPost
                    .computeIfAbsent(
                            row.getPostId(),
                            ignored -> new ArrayList<>()
                    )
                    .add(row.getTechnologyName());
        }

        // 10. Fetch all tags for the selected posts.
        Map<String, List<String>> tagsByPost = new HashMap<>();

        var tagRows = postRepository.findFeedTagsByPostIds(postIds);

        for (var row : tagRows) {

            tagsByPost
                    .computeIfAbsent(
                            row.getPostId(),
                            ignored -> new ArrayList<>()
                    )
                    .add(row.getTag());
        }

        // 11. Fetch liked post IDs for the current user.
        Set<String> likedPostIds = new HashSet<>(
                postLikeRepository.findLikedPostIds(userId, postIds)
        );

        // 12. Assemble the final feed in the original database order.
        List<PostFeedItemResponse> posts = new ArrayList<>(pageRows.size());

        for (String postId : postIds) {

            var row = postsById.get(postId);

            if (row == null) {
                continue;
            }

            List<String> images = imagesByPost.getOrDefault(
                    postId,
                    List.of()
            );

            // Fallback for older posts without PostImage records.
            if (images.isEmpty() && row.getThumbnailUrl() != null) {
                images = List.of(row.getThumbnailUrl());
            }

            PostAuthorResponse author = new PostAuthorResponse(
                    row.getAuthorId(),
                    row.getAuthorUsername(),
                    row.getAuthorName(),
                    technologiesByUser.getOrDefault(
                            row.getAuthorId(),
                            List.of()
                    )
            );

            PostFeedItemResponse item = new PostFeedItemResponse(
                    row.getPostId(),
                    row.getTitle(),
                    row.getDescription(),
                    row.getThumbnailUrl(),
                    images,
                    row.getGithubUrl(),
                    row.getLiveProjectUrl(),
                    technologiesByPost.getOrDefault(postId, List.of()),
                    tagsByPost.getOrDefault(postId, List.of()),
                    author,
                    row.getLikesCount(),
                    row.getCommentsCount(),
                    likedPostIds.contains(postId),
                    row.getCreatedAt()
            );

            posts.add(item);
        }

        // 13. Generate the next cursor from the last returned post.
        String nextCursor = null;

        if (hasMore) {

            var lastRow = pageRows.get(pageRows.size() - 1);

            nextCursor = encodeCursor(
                    lastRow.getCreatedAt(),
                    lastRow.getPostId()
            );
        }

        return new PostFeedResponse(
                posts,
                nextCursor,
                hasMore
        );
    }

    private String encodeCursor(
            Instant createdAt,
            String postId
    ) {

        String rawCursor = createdAt + "|" + postId;

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        rawCursor.getBytes(StandardCharsets.UTF_8)
                );
    }

    private FeedCursor decodeCursor(String cursor) {

        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        if (cursor.length() > 512) {
            throw invalidCursor();
        }

        try {

            byte[] decodedBytes =
                    Base64.getUrlDecoder().decode(cursor);

            String decoded = new String(
                    decodedBytes,
                    StandardCharsets.UTF_8
            );

            int separator = decoded.indexOf('|');

            if (separator <= 0 || separator == decoded.length() - 1) {
                throw invalidCursor();
            }

            Instant createdAt = Instant.parse(
                    decoded.substring(0, separator)
            );

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

    private record FeedCursor(
            Instant createdAt,
            String postId
    ) {}
}
