package com.neong.vixie.services.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Generic file storage interface. Implementations may target
 * Firebase Storage, AWS S3, or local filesystem.
 */
public interface StorageService {

    /**
     * Upload an avatar image and return the public download URL.
     *
     * @param file   the uploaded multipart file
     * @param userId the owning user's ID (used for path namespacing)
     * @return the publicly-accessible URL of the uploaded file
     */
    String uploadAvatar(MultipartFile file, String userId);
}
