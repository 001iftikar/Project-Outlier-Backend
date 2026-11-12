
package com.iftikar.outlier.integration.cloudinary;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryStorageService {

    private static final String POSTS_ASSET_FOLDER = "outlier/posts";

    private static final List<String> ALLOWED_FORMATS =
            List.of("jpg", "jpeg", "png", "webp");

    private final Cloudinary cloudinary;

    public CloudinaryImageUpload uploadImage(MultipartFile file) {

        validateImage(file);

        Map<String, Object> uploadOptions = Map.of(
                "resource_type", "image",
                "asset_folder", POSTS_ASSET_FOLDER,
                "allowed_formats", ALLOWED_FORMATS
        );

        Map<?, ?> uploadResult;

        try {
            uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    uploadOptions
            );

        } catch (IOException | RuntimeException exception) {

            throw new CloudinaryStorageException(
                    "Failed to upload image to Cloudinary.",
                    exception
            );
        }

        Object publicIdValue = uploadResult.get("public_id");
        Object secureUrlValue = uploadResult.get("secure_url");

        if (!(publicIdValue instanceof String)
                || !(secureUrlValue instanceof String)) {

            throw new CloudinaryStorageException(
                    "Cloudinary returned an incomplete upload response."
            );
        }

        return new CloudinaryImageUpload(
                (String) publicIdValue,
                (String) secureUrlValue
        );
    }

    private void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Image file cannot be empty."
            );
        }

        String contentType = file.getContentType();

        if (contentType != null
                && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {

            throw new IllegalArgumentException(
                    "Only image files are accepted."
            );
        }
    }

    public void deleteImage(String publicId) {

        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(
                    publicId,
                    Map.of("resource_type", "image")
            );

        } catch (Exception exception) {

            throw new CloudinaryStorageException(
                    "Failed to delete image from Cloudinary.",
                    exception
            );
        }
    }
}
