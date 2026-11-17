package com.iftikar.outlier.controller;

import com.iftikar.outlier.dto.CommentDeleteResponse;
import com.iftikar.outlier.dto.CommentMutationResponse;
import com.iftikar.outlier.dto.CommentPageResponse;
import com.iftikar.outlier.dto.CommentRequest;
import com.iftikar.outlier.result.ApiResponse;
import com.iftikar.outlier.service.api.CommentService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;


    // CREATE COMMENT

    @PostMapping
    public ResponseEntity<ApiResponse<CommentMutationResponse>> createComment(

            @AuthenticationPrincipal String userId,

            @PathVariable String postId,

            @Valid
            @RequestBody
            CommentRequest request

    ) {

        CommentMutationResponse result =
                commentService.createComment(
                        userId,
                        postId,
                        request.content()
                );

        ApiResponse<CommentMutationResponse> response =
                new ApiResponse<>(
                        "SUCCESS",
                        "Comment added successfully.",
                        result
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // GET COMMENTS

    @GetMapping
    public ResponseEntity<ApiResponse<CommentPageResponse>> getComments(

            @PathVariable String postId,

            @RequestParam(required = false) String cursor,

            @RequestParam(defaultValue = "20") int size

    ) {

        CommentPageResponse result =
                commentService.getComments(
                        postId,
                        cursor,
                        size
                );

        ApiResponse<CommentPageResponse> response =
                new ApiResponse<>(
                        "SUCCESS",
                        "Comments retrieved successfully.",
                        result
                );

        return ResponseEntity.ok(response);
    }


    // UPDATE COMMENT

    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentMutationResponse>> updateComment(

            @AuthenticationPrincipal String userId,

            @PathVariable String postId,

            @PathVariable String commentId,

            @Valid
            @RequestBody
            CommentRequest request

    ) {

        CommentMutationResponse result =
                commentService.updateComment(
                        userId,
                        postId,
                        commentId,
                        request.content()
                );

        ApiResponse<CommentMutationResponse> response =
                new ApiResponse<>(
                        "SUCCESS",
                        "Comment updated successfully.",
                        result
                );

        return ResponseEntity.ok(response);
    }


    // DELETE COMMENT

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentDeleteResponse>> deleteComment(

            @AuthenticationPrincipal String userId,

            @PathVariable String postId,

            @PathVariable String commentId

    ) {

        CommentDeleteResponse result =
                commentService.deleteComment(
                        userId,
                        postId,
                        commentId
                );

        ApiResponse<CommentDeleteResponse> response =
                new ApiResponse<>(
                        "SUCCESS",
                        "Comment deleted successfully.",
                        result
                );

        return ResponseEntity.ok(response);
    }
}