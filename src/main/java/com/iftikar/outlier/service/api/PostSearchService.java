package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.PostSearchResponse;

public interface PostSearchService {

    PostSearchResponse searchPosts(
            String currentUserId,
            String query,
            String cursor,
            String matchedTechStackInsteadCursor,
            int size
    );
}
