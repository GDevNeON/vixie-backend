package com.neong.vixie.service;

/**
 * Interface for content storage operations.
 * Implementations may use Cloudinary, S3, Firebase, etc.
 */
public interface ContentStorageService {

    /**
     * Upload a file to storage.
     *
     * @param data       file bytes
     * @param folderPath folder path (e.g., "creators/{creatorId}/{uuid}/")
     * @param fileName   the file name
     * @return the CDN URL of the uploaded file
     */
    String upload(byte[] data, String folderPath, String fileName);

    /**
     * Delete a file from storage by its public ID or URL.
     */
    void delete(String publicIdOrUrl);
}
