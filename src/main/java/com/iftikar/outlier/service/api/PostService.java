package com.iftikar.outlier.service.api;

import com.iftikar.outlier.dto.CreatePostRequest;
import com.iftikar.outlier.dto.CreatePostResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostService {
    CreatePostResponse createPost(
            String userId,
            CreatePostRequest request,
            List<MultipartFile> images
    );
}
