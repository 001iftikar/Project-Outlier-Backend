package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.PostFeedResponse;

public interface PostFeedService {

    PostFeedResponse getFeed(
            String userId,
            String cursor,
            int size
    );
}