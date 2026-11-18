package com.iftikar.outlier.dto;

import java.util.List;

/**
 * A post returned for a hashtag search because one or more technologies
 * matched the searched hashtag terms, but none of the post's tags did.
 */
public record MatchedTechStackInsteadResponse(
        PostFeedItemResponse post,
        List<String> matchedTechStack
) {}
