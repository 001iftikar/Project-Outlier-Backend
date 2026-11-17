package com.iftikar.outlier.dto;

import java.util.List;

public record CommentPageResponse(
        List<CommentResponse> comments,
        String nextCursor,
        boolean hasMore,
        long commentsCount
) {}