package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.PostAuthorResponse;
import com.iftikar.outlier.dto.PostFeedItemResponse;
import com.iftikar.outlier.repository.PostImageRepository;
import com.iftikar.outlier.repository.PostLikeRepository;
import com.iftikar.outlier.repository.PostRepository;
import com.iftikar.outlier.repository.PostTechnologyRepository;
import com.iftikar.outlier.repository.UserTechnologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds complete post feed items for a known, ordered list of post IDs.
 * Both the normal feed and search use this component to avoid duplicating
 * all the batch-fetching and response-mapping logic.
 */
@Component
@RequiredArgsConstructor
public class PostResponseAssembler {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostTechnologyRepository postTechnologyRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserTechnologyRepository userTechnologyRepository;

    public List<PostFeedItemResponse> assemble(
            String currentUserId,
            List<String> postIds
    ) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }

        // Fetch the basic post and author details in one query.
        List<PostRepository.FeedPostProjection> baseRows =
                postRepository.findFeedPostsByIds(postIds);

        Map<String, PostRepository.FeedPostProjection> postsById =
                new HashMap<>();

        for (var row : baseRows) {
            postsById.put(row.getPostId(), row);
        }

        // Fetch author technologies in one query for all authors in this batch.
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

        // Fetch all images for the selected posts.
        Map<String, List<String>> imagesByPost = new HashMap<>();

        var imageRows = postImageRepository.findFeedImagesByPostIds(postIds);

        for (var row : imageRows) {
            imagesByPost
                    .computeIfAbsent(
                            row.getPostId(),
                            ignored -> new ArrayList<>()
                    )
                    .add(row.getImageUrl());
        }

        // Fetch all technologies for the selected posts.
        Map<String, List<String>> technologiesByPost = new HashMap<>();

        var technologyRows =
                postTechnologyRepository.findFeedTechnologiesByPostIds(postIds);

        for (var row : technologyRows) {
            technologiesByPost
                    .computeIfAbsent(
                            row.getPostId(),
                            ignored -> new ArrayList<>()
                    )
                    .add(row.getTechnologyName());
        }

        // Fetch all tags for the selected posts.
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

        // Fetch liked post IDs for the current user.
        Set<String> likedPostIds = new HashSet<>(
                postLikeRepository.findLikedPostIds(currentUserId, postIds)
        );

        // Keep exactly the order in which the IDs were supplied.
        List<PostFeedItemResponse> result = new ArrayList<>(postIds.size());

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

            result.add(new PostFeedItemResponse(
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
            ));
        }

        return result;
    }
}
