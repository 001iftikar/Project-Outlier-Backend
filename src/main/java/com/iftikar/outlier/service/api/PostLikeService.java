package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.PostLikeResponse;

public interface PostLikeService {

    PostLikeResponse likePost(
            String userId,
            String postId
    );

    PostLikeResponse unlikePost(
            String userId,
            String postId
    );
}