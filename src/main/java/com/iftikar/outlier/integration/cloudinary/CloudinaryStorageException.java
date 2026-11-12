package com.iftikar.outlier.integration.cloudinary;

public class CloudinaryStorageException extends RuntimeException {

    public CloudinaryStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudinaryStorageException(String message) {
        super(message);
    }
}
