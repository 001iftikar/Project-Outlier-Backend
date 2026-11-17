package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.*;
import com.iftikar.outlier.entity.Comment;
import com.iftikar.outlier.entity.Post;
import com.iftikar.outlier.entity.User;
import com.iftikar.outlier.repository.CommentRepository;
import com.iftikar.outlier.repository.PostRepository;
import com.iftikar.outlier.repository.UserRepository;
import com.iftikar.outlier.result.ApiException;
import com.iftikar.outlier.service.api.CommentService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;


    // ---------------------------------------------------------
    // CREATE COMMENT
    // ---------------------------------------------------------

    @Override
    @Transactional
    public CommentMutationResponse createComment(
            String userId,
            String postId,
            String content
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

        // 3. Create the comment.
        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(content.trim())
                .build();

        // 4. Save the comment.
        commentRepository.save(comment);

        // 5. Increment the post's comment count.
        post.setCommentsCount(post.getCommentsCount() + 1);

        // 6. Build the response.
        return new CommentMutationResponse(
                toResponse(comment, user),
                post.getCommentsCount()
        );
    }


    // ---------------------------------------------------------
    // GET COMMENTS
    // ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public CommentPageResponse getComments(
            String postId,
            String cursor,
            int size
    ) {

        // 1. Validate page size.
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(
                    "INVALID_PAGE_SIZE",
                    "Page size must be between 1 and 50.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 2. Verify that the post exists.
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(
                        "POST_NOT_FOUND",
                        "The requested post was not found.",
                        HttpStatus.NOT_FOUND
                ));

        // 3. Decode the cursor.
        CommentCursor decodedCursor = decodeCursor(cursor);

        // 4. Fetch one extra comment to determine hasMore.
        List<CommentRepository.CommentProjection> fetchedRows;

        if (decodedCursor == null) {

            fetchedRows = commentRepository.findFirstPage(
                    postId,
                    PageRequest.of(0, size + 1)
            );

        } else {

            fetchedRows = commentRepository.findPageAfter(
                    postId,
                    decodedCursor.createdAt(),
                    decodedCursor.commentId(),
                    PageRequest.of(0, size + 1)
            );
        }

        // 5. Determine whether another page exists.
        boolean hasMore = fetchedRows.size() > size;

        // 6. Remove the extra row.
        List<CommentRepository.CommentProjection> pageRows =
                hasMore
                        ? fetchedRows.subList(0, size)
                        : fetchedRows;

        // 7. Convert projections to response DTOs.
        List<CommentResponse> comments = new ArrayList<>(
                pageRows.size()
        );

        for (var row : pageRows) {

            CommentAuthorResponse author =
                    new CommentAuthorResponse(
                            row.getAuthorId(),
                            row.getAuthorUsername(),
                            row.getAuthorName()
                    );

            comments.add(
                    new CommentResponse(
                            row.getCommentId(),
                            row.getContent(),
                            author,
                            row.getCreatedAt(),
                            row.getUpdatedAt()
                    )
            );
        }

        // 8. Generate the next cursor.
        String nextCursor = null;

        if (hasMore) {

            var lastRow = pageRows.get(pageRows.size() - 1);

            nextCursor = encodeCursor(
                    lastRow.getCreatedAt(),
                    lastRow.getCommentId()
            );
        }

        return new CommentPageResponse(
                comments,
                nextCursor,
                hasMore,
                post.getCommentsCount()
        );
    }


    // ---------------------------------------------------------
    // UPDATE COMMENT
    // ---------------------------------------------------------

    @Override
    @Transactional
    public CommentMutationResponse updateComment(
            String userId,
            String postId,
            String commentId,
            String content
    ) {

        // Lock the parent post first to maintain consistent
        // lock ordering across comment mutations.
        Post post = getPostForUpdate(postId);

        // Lock the requested comment.
        Comment comment = getCommentForUpdate(
                commentId,
                postId
        );

        // Only the comment author can edit it.
        if (!comment.getUser().getId().equals(userId)) {
            throw new ApiException(
                    "COMMENT_EDIT_FORBIDDEN",
                    "You can only edit your own comments.",
                    HttpStatus.FORBIDDEN
            );
        }

        // Update content.
        comment.setContent(content.trim());
        commentRepository.flush();

        // updatedAt is maintained by Comment's @PreUpdate.

        return new CommentMutationResponse(
                toResponse(comment, comment.getUser()),
                post.getCommentsCount()
        );
    }


    // ---------------------------------------------------------
    // DELETE COMMENT
    // ---------------------------------------------------------

    @Override
    @Transactional
    public CommentDeleteResponse deleteComment(
            String userId,
            String postId,
            String commentId
    ) {

        // Lock the parent post first.
        Post post = getPostForUpdate(postId);

        // Lock the requested comment.
        Comment comment = getCommentForUpdate(
                commentId,
                postId
        );

        boolean isCommentAuthor =
                comment.getUser().getId().equals(userId);

        boolean isPostAuthor =
                post.getUser().getId().equals(userId);

        // The comment author or post author can delete it.
        if (!isCommentAuthor && !isPostAuthor) {
            throw new ApiException(
                    "COMMENT_DELETE_FORBIDDEN",
                    "You are not allowed to delete this comment.",
                    HttpStatus.FORBIDDEN
            );
        }

        // Delete the comment.
        commentRepository.delete(comment);

        // Decrement the comment count.
        post.setCommentsCount(
                Math.max(0, post.getCommentsCount() - 1)
        );

        return new CommentDeleteResponse(
                post.getCommentsCount()
        );
    }


    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------

    private Post getPostForUpdate(String postId) {

        return postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new ApiException(
                        "POST_NOT_FOUND",
                        "The requested post was not found.",
                        HttpStatus.NOT_FOUND
                ));
    }


    private Comment getCommentForUpdate(
            String commentId,
            String postId
    ) {

        return commentRepository.findByIdForUpdate(
                        commentId,
                        postId
                )
                .orElseThrow(() -> new ApiException(
                        "COMMENT_NOT_FOUND",
                        "The requested comment was not found.",
                        HttpStatus.NOT_FOUND
                ));
    }


    private CommentResponse toResponse(
            Comment comment,
            User user
    ) {

        CommentAuthorResponse author =
                new CommentAuthorResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getName()
                );

        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                author,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }


    private String encodeCursor(
            Instant createdAt,
            String commentId
    ) {

        String rawCursor = createdAt + "|" + commentId;

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        rawCursor.getBytes(StandardCharsets.UTF_8)
                );
    }


    private CommentCursor decodeCursor(String cursor) {

        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        if (cursor.length() > 512) {
            throw invalidCursor();
        }

        try {

            byte[] decodedBytes =
                    Base64.getUrlDecoder().decode(cursor);

            String decoded = new String(
                    decodedBytes,
                    StandardCharsets.UTF_8
            );

            int separator = decoded.indexOf('|');

            if (separator <= 0 || separator == decoded.length() - 1) {
                throw invalidCursor();
            }

            Instant createdAt = Instant.parse(
                    decoded.substring(0, separator)
            );

            String commentId = decoded.substring(separator + 1);

            if (commentId.isBlank() || commentId.length() > 64) {
                throw invalidCursor();
            }

            return new CommentCursor(createdAt, commentId);

        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw invalidCursor();
        }
    }


    private ApiException invalidCursor() {

        return new ApiException(
                "INVALID_CURSOR",
                "The supplied comment cursor is invalid.",
                HttpStatus.BAD_REQUEST
        );
    }


    private record CommentCursor(
            Instant createdAt,
            String commentId
    ) {}
}