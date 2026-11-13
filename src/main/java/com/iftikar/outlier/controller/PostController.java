package com.iftikar.outlier.controller;

import com.iftikar.outlier.dto.CreatePostRequest;
import com.iftikar.outlier.dto.CreatePostResponse;
import com.iftikar.outlier.dto.PostFeedResponse;
import com.iftikar.outlier.dto.PostLikeResponse;
import com.iftikar.outlier.result.ApiResponse;
import com.iftikar.outlier.service.api.PostFeedService;
import com.iftikar.outlier.service.api.PostLikeService;
import com.iftikar.outlier.service.api.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostFeedService postFeedService;
    private final PostLikeService postLikeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CreatePostResponse>> createPost(

            @AuthenticationPrincipal String userId,

            @Valid
            @RequestPart("request")
            CreatePostRequest request,

            @RequestPart(value = "images", required = false)
            List<MultipartFile> images

    ) {

        CreatePostResponse result = postService.createPost(
                userId,
                request,
                images
        );

        ApiResponse<CreatePostResponse> response =
                new ApiResponse<>(
                        "SUCCESS",
                        "Post created successfully.",
                        result
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PostFeedResponse>> getPosts(

            @AuthenticationPrincipal String userId,

            @RequestParam(required = false) String cursor,

            @RequestParam(defaultValue = "20") int size

    ) {

        PostFeedResponse result = postFeedService.getFeed(
                userId,
                cursor,
                size
        );

        ApiResponse<PostFeedResponse> response =
                new ApiResponse<>(
                        "SUCCESS",
                        "Posts retrieved successfully.",
                        result
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<PostLikeResponse>> likePost(

            @AuthenticationPrincipal String userId,

            @PathVariable String postId

    ) {

        PostLikeResponse result = postLikeService.likePost(
                userId,
                postId
        );

        ApiResponse<PostLikeResponse> response =
                new ApiResponse<>(
                        "SUCCESS",
                        "Post is liked.",
                        result
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<PostLikeResponse>> unlikePost(

            @AuthenticationPrincipal String userId,

            @PathVariable String postId

    ) {

        PostLikeResponse result = postLikeService.unlikePost(
                userId,
                postId
        );

        ApiResponse<PostLikeResponse> response =
                new ApiResponse<>(
                        "SUCCESS",
                        "Post is not liked.",
                        result
                );

        return ResponseEntity.ok(response);
    }
}