package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.CommentDeleteResponse;
import com.iftikar.outlier.dto.CommentMutationResponse;
import com.iftikar.outlier.dto.CommentPageResponse;

public interface CommentService {

    CommentMutationResponse createComment(
            String userId,
            String postId,
            String content
    );

    CommentPageResponse getComments(
            String postId,
            String cursor,
            int size
    );

    CommentMutationResponse updateComment(
            String userId,
            String postId,
            String commentId,
            String content
    );

    CommentDeleteResponse deleteComment(
            String userId,
            String postId,
            String commentId
    );
}