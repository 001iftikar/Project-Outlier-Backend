package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.PostLikeResponse;
import com.iftikar.outlier.entity.Post;
import com.iftikar.outlier.entity.PostLike;
import com.iftikar.outlier.entity.PostLikeId;
import com.iftikar.outlier.entity.User;
import com.iftikar.outlier.repository.PostLikeRepository;
import com.iftikar.outlier.repository.PostRepository;
import com.iftikar.outlier.repository.UserRepository;
import com.iftikar.outlier.result.ApiException;
import com.iftikar.outlier.service.api.PostLikeService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PostLikeServiceImpl implements PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PostLikeResponse likePost(
            String userId,
            String postId
    ) {

        // 1. Lock the post row.
        Post post = getPostForUpdate(postId);

        // 2. Verify that the authenticated user exists.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        "USER_NOT_FOUND",
                        "Authenticated user was not found.",
                        HttpStatus.NOT_FOUND
                ));

        // 3. Build the composite primary key.
        PostLikeId likeId = new PostLikeId(
                postId,
                userId
        );

        // 4. Check whether this user already liked this post.
        boolean alreadyLiked = postLikeRepository.existsById(likeId);

        if (!alreadyLiked) {

            // 5. Create the like record.
            PostLike postLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .createdAt(Instant.now())
                    .build();

            postLikeRepository.save(postLike);

            // 6. Increment the post's like count.
            post.setLikesCount(post.getLikesCount() + 1);
        }

        // 7. Return the current state.
        return new PostLikeResponse(
                true,
                post.getLikesCount()
        );
    }

    @Override
    @Transactional
    public PostLikeResponse unlikePost(
            String userId,
            String postId
    ) {

        // 1. Lock the post row.
        Post post = getPostForUpdate(postId);

        // 2. Build the composite primary key.
        PostLikeId likeId = new PostLikeId(
                postId,
                userId
        );

        // 3. Check whether the like exists.
        boolean alreadyLiked = postLikeRepository.existsById(likeId);

        if (alreadyLiked) {

            // 4. Delete the like record.
            postLikeRepository.deleteById(likeId);

            // 5. Decrement the like count.
            post.setLikesCount(
                    Math.max(0, post.getLikesCount() - 1)
            );
        }

        // 6. Return the current state.
        return new PostLikeResponse(
                false,
                post.getLikesCount()
        );
    }

    private Post getPostForUpdate(String postId) {

        return postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new ApiException(
                        "POST_NOT_FOUND",
                        "The requested post was not found.",
                        HttpStatus.NOT_FOUND
                ));
    }
}