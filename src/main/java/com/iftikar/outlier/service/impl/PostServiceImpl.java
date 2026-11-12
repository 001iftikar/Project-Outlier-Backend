
package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.CreatePostRequest;
import com.iftikar.outlier.dto.CreatePostResponse;
import com.iftikar.outlier.dto.GithubRepositoryCheckResponse;
import com.iftikar.outlier.dto.LiveUrlCheckResponse;
import com.iftikar.outlier.entity.User;
import com.iftikar.outlier.integration.cloudinary.CloudinaryImageUpload;
import com.iftikar.outlier.integration.cloudinary.CloudinaryStorageException;
import com.iftikar.outlier.integration.cloudinary.CloudinaryStorageService;
import com.iftikar.outlier.repository.UserRepository;
import com.iftikar.outlier.result.ApiException;
import com.iftikar.outlier.service.api.PostService;
import com.iftikar.outlier.service.api.UrlValidationService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final Logger log =
            LoggerFactory.getLogger(PostServiceImpl.class);

    private static final int MAX_IMAGES = 3;

    private static final long MAX_IMAGE_SIZE =
            10L * 1024 * 1024;

    private final UserRepository userRepository;
    private final UrlValidationService urlValidationService;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final PostPersistenceService postPersistenceService;

    @Override
    public CreatePostResponse createPost(
            String userId,
            CreatePostRequest request,
            List<MultipartFile> images
    ) {

        // 1. Verify the authenticated user.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        "USER_NOT_FOUND",
                        "Authenticated user was not found.",
                        HttpStatus.NOT_FOUND
                ));

        // 2. Only developers can create posts.
        if (!"DEVELOPER".equals(user.getRole())) {
            throw new ApiException(
                    "FORBIDDEN",
                    "Only developers can create posts.",
                    HttpStatus.FORBIDDEN
            );
        }

        // 3. Validate the selected images.
        validateImages(images);

        // 4. Validate the GitHub repository, if supplied.
        String githubUrl = validateGithubRepository(request.githubUrl());

        // 5. Normalize the optional live project URL.
        String liveProjectUrl = normalizeOptionalUrl(
                request.liveProjectUrl()
        );

        // 6. Check GitHub URL reputation.
        if (githubUrl != null) {
            ensureUrlIsSafe(githubUrl, "GitHub repository URL");
        }

        // 7. Check live project URL reputation.
        if (liveProjectUrl != null) {
            ensureUrlIsSafe(liveProjectUrl, "Live project URL");
        }

        // 8. Upload images and keep their Cloudinary metadata.
        List<CloudinaryImageUpload> uploadedImages =
                new ArrayList<>(images.size());

        try {

            for (MultipartFile image : images) {

                CloudinaryImageUpload uploadedImage =
                        cloudinaryStorageService.uploadImage(image);

                uploadedImages.add(uploadedImage);
            }

            // 9. Persist the post and all related database records.
            return postPersistenceService.persistPost(
                    userId,
                    request,
                    githubUrl,
                    liveProjectUrl,
                    uploadedImages
            );

        } catch (RuntimeException exception) {

            // 10. Remove images if uploading or database persistence fails.
            cleanupUploadedImages(uploadedImages);

            if (exception instanceof CloudinaryStorageException) {

                log.error("Cloudinary image upload failed.", exception);

                throw new ApiException(
                        "IMAGE_UPLOAD_FAILED",
                        "Could not upload images. Please try again.",
                        HttpStatus.BAD_GATEWAY
                );
            }

            throw exception;
        }
    }

    private String validateGithubRepository(String rawUrl) {

        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        GithubRepositoryCheckResponse result =
                urlValidationService.checkRepository(rawUrl.strip());

        return switch (result.status()) {

            case VALID -> {

                if (result.normalizedUrl() == null
                        || result.normalizedUrl().isBlank()) {

                    throw new ApiException(
                            "GITHUB_VALIDATION_FAILED",
                            "Could not obtain the verified repository URL.",
                            HttpStatus.SERVICE_UNAVAILABLE
                    );
                }

                yield result.normalizedUrl();
            }

            case NOT_FOUND_OR_PRIVATE -> throw new ApiException(
                    "GITHUB_REPOSITORY_NOT_FOUND",
                    "The GitHub repository does not exist or is private.",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );

            case REPOSITORY_MOVED -> throw new ApiException(
                    "GITHUB_REPOSITORY_MOVED",
                    "The GitHub repository has moved. Provide its current URL.",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        };
    }

    private void ensureUrlIsSafe(String url, String description) {

        LiveUrlCheckResponse result =
                urlValidationService.checkLiveUrl(url);

        switch (result.status()) {

            case SAFE -> {
                if (!result.safe()) {
                    throw new ApiException(
                            "URL_CHECK_UNAVAILABLE",
                            "Could not reliably verify the " + description + ".",
                            HttpStatus.SERVICE_UNAVAILABLE
                    );
                }
            }

            case UNSAFE -> throw new ApiException(
                    "UNSAFE_URL",
                    "The " + description + " was identified as potentially harmful.",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );

            case UNVERIFIED -> throw new ApiException(
                    "URL_CHECK_UNAVAILABLE",
                    "Could not verify the " + description + " right now. Try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    private String normalizeOptionalUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        return url.strip();
    }

    private void validateImages(List<MultipartFile> images) {

        if (images == null || images.isEmpty()) {
            throw new ApiException(
                    "IMAGES_REQUIRED",
                    "At least one image is required.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (images.size() > MAX_IMAGES) {
            throw new ApiException(
                    "TOO_MANY_IMAGES",
                    "Maximum 3 images are allowed per post.",
                    HttpStatus.BAD_REQUEST
            );
        }

        for (MultipartFile image : images) {

            if (image == null || image.isEmpty()) {
                throw new ApiException(
                        "EMPTY_IMAGE",
                        "One or more uploaded images are empty.",
                        HttpStatus.BAD_REQUEST
                );
            }

            if (image.getSize() > MAX_IMAGE_SIZE) {
                throw new ApiException(
                        "IMAGE_TOO_LARGE",
                        "Each image must be smaller than 10 MB.",
                        HttpStatus.PAYLOAD_TOO_LARGE
                );
            }

            String contentType = image.getContentType();

            if (contentType != null
                    && !contentType.toLowerCase(Locale.ROOT)
                    .startsWith("image/")) {

                throw new ApiException(
                        "INVALID_IMAGE_TYPE",
                        "Only image files are accepted.",
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE
                );
            }
        }
    }

    private void cleanupUploadedImages(
            List<CloudinaryImageUpload> uploadedImages
    ) {

        for (CloudinaryImageUpload image : uploadedImages) {

            try {
                cloudinaryStorageService.deleteImage(image.publicId());

            } catch (RuntimeException cleanupException) {

                log.error(
                        "Failed to clean up Cloudinary asset: {}",
                        image.publicId(),
                        cleanupException
                );
            }
        }
    }
}
