
package com.iftikar.outlier.service.impl;

import com.iftikar.outlier.dto.CreatePostRequest;
import com.iftikar.outlier.dto.CreatePostResponse;
import com.iftikar.outlier.entity.Post;
import com.iftikar.outlier.entity.PostImage;
import com.iftikar.outlier.entity.PostTechnology;
import com.iftikar.outlier.entity.Technology;
import com.iftikar.outlier.entity.User;
import com.iftikar.outlier.integration.cloudinary.CloudinaryImageUpload;
import com.iftikar.outlier.repository.PostImageRepository;
import com.iftikar.outlier.repository.PostRepository;
import com.iftikar.outlier.repository.PostTechnologyRepository;
import com.iftikar.outlier.repository.TechnologyRepository;
import com.iftikar.outlier.repository.UserRepository;
import com.iftikar.outlier.result.ApiException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostPersistenceService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final TechnologyRepository technologyRepository;
    private final PostTechnologyRepository postTechnologyRepository;

    @Transactional
    public CreatePostResponse persistPost(
            String userId,
            CreatePostRequest request,
            String githubUrl,
            String liveProjectUrl,
            List<CloudinaryImageUpload> uploadedImages
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        "USER_NOT_FOUND",
                        "Authenticated user was not found.",
                        HttpStatus.NOT_FOUND
                ));

        if (!"DEVELOPER".equals(user.getRole())) {
            throw new ApiException(
                    "FORBIDDEN",
                    "Only developers can create posts.",
                    HttpStatus.FORBIDDEN
            );
        }

        Post post = Post.builder()
                .user(user)
                .title(request.title().strip())
                .description(request.description().strip())
                .thumbnailUrl(uploadedImages.get(0).secureUrl())
                .githubUrl(githubUrl)
                .liveProjectUrl(liveProjectUrl)
                .tags(normalizeUniqueValues(request.tags()))
                .build();

        Post savedPost = postRepository.save(post);

        List<PostImage> postImages = new ArrayList<>();

        for (int index = 0; index < uploadedImages.size(); index++) {

            CloudinaryImageUpload uploadedImage = uploadedImages.get(index);

            PostImage postImage = PostImage.builder()
                    .post(savedPost)
                    .imageUrl(uploadedImage.secureUrl())
                    .publicId(uploadedImage.publicId())
                    .position(index)
                    .build();

            postImages.add(postImage);
        }

        postImageRepository.saveAll(postImages);

        List<PostTechnology> postTechnologies = new ArrayList<>();

        List<String> technologyNames =
                normalizeUniqueValues(request.techStack());

        for (String technologyName : technologyNames) {

            Technology technology = technologyRepository
                    .findByNameIgnoreCase(technologyName)
                    .orElseGet(() -> technologyRepository.save(
                            Technology.builder()
                                    .name(technologyName)
                                    .build()
                    ));

            PostTechnology postTechnology = PostTechnology.builder()
                    .post(savedPost)
                    .technology(technology)
                    .build();

            postTechnologies.add(postTechnology);
        }

        if (!postTechnologies.isEmpty()) {
            postTechnologyRepository.saveAll(postTechnologies);
        }

        return new CreatePostResponse(
                savedPost.getId(),
                savedPost.getThumbnailUrl()
        );
    }

    private List<String> normalizeUniqueValues(List<String> values) {

        if (values == null || values.isEmpty()) {
            return List.of();
        }

        Map<String, String> uniqueValues = new LinkedHashMap<>();

        for (String value : values) {

            if (value == null || value.isBlank()) {
                continue;
            }

            String normalized = value.strip();

            String key = normalized.toLowerCase(Locale.ROOT);

            uniqueValues.putIfAbsent(key, normalized);
        }

        return new ArrayList<>(uniqueValues.values());
    }
}
