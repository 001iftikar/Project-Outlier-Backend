package com.iftikar.outlier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(

        @NotBlank(message = "Title is required.")
        @Size(max = 200, message = "Title cannot exceed 200 characters.")
        String title,

        @NotBlank(message = "Description is required.")
        @Size(max = 10000, message = "Description is too long.")
        String description,

        @Size(max = 500, message = "GitHub URL is too long.")
        String githubUrl,

        @Size(max = 500, message = "Live project URL is too long.")
        String liveProjectUrl,

        @Size(max = 20, message = "Maximum 20 technologies allowed.")
        List<
                @NotBlank(message = "Technology name cannot be blank.")
                @Size(max = 100, message = "Technology name is too long.")
                        String
                > techStack,

        @Size(max = 10, message = "Maximum 10 tags allowed.")
        List<
                @NotBlank(message = "Tag cannot be blank.")
                @Size(max = 50, message = "Tag cannot exceed 50 characters.")
                        String
                > tags

) {}
