package com.iftikar.outlier.dto;

import java.time.Instant;

public record CommentResponse(
        String id,
        String content,
        CommentAuthorResponse author,
        Instant createdAt,
        Instant updatedAt
) {}