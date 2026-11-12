
package com.iftikar.outlier.controller;

import com.iftikar.outlier.dto.CreatePostRequest;
import com.iftikar.outlier.dto.CreatePostResponse;
import com.iftikar.outlier.result.ApiResponse;
import com.iftikar.outlier.service.api.PostFeedService;
import com.iftikar.outlier.service.api.PostService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.iftikar.outlier.dto.PostFeedResponse;
import com.iftikar.outlier.service.api.PostFeedService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostFeedService postFeedService;

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
}
