package com.iftikar.outlier.dto;

public record CommentMutationResponse(
        CommentResponse comment,
        long commentsCount
) {
}