package com.iftikar.outlier.dto;

import java.time.Instant;
import java.util.List;

public record PostFeedItemResponse(
        String id,
        String title,
        String description,
        String thumbnailUrl,
        List<String> images,
        String githubUrl,
        String liveProjectUrl,
        List<String> techStack,
        List<String> tags,
        PostAuthorResponse user,
        long likesCount,
        long commentsCount,
        boolean likedByCurrentUser,
        Instant createdAt
) {}