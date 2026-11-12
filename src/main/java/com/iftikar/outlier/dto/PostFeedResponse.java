package com.iftikar.outlier.dto;

import java.util.List;

public record PostFeedResponse(
        List<PostFeedItemResponse> posts,
        String nextCursor,
        boolean hasMore
) {}