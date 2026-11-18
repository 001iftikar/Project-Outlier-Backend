package com.iftikar.outlier.dto;

import java.util.List;

public record PostSearchResponse(
        String searchMode,
        List<String> matchedTerms,
        List<PostFeedItemResponse> posts,
        List<MatchedTechStackInsteadResponse> matchedTechStackInstead,
        String nextCursor,
        boolean hasMore,
        String nextMatchedTechStackInsteadCursor,
        boolean hasMoreMatchedTechStackInstead,
        boolean usedTechStackFallback
) {}
