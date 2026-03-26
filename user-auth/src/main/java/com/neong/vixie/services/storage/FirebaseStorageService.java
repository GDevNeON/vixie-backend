package com.neong.vixie.services.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class FirebaseStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseStorageService.class);

    @Override
    public String uploadAvatar(MultipartFile file, String userId) {
        try {
            String bucketName = StorageClient.getInstance().bucket().getName();
            Storage storage = StorageClient.getInstance().bucket().getStorage();

            // Generate unique file path: avatars/{userId}_{uuid}.{ext}
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = "avatars/" + userId + "_" + UUID.randomUUID() + extension;

            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            Blob blob = storage.create(blobInfo, file.getBytes());

            // Construct the public Firebase Storage download URL
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            String downloadUrl = String.format(
                    "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                    bucketName, encodedFileName);

            log.info("Avatar uploaded for user_id={}, path={}", userId, fileName);
            return downloadUrl;

        } catch (IOException e) {
            log.error("Failed to upload avatar for user_id={}", userId, e);
            throw new RuntimeException("Failed to upload avatar file", e);
        }
    }
}
