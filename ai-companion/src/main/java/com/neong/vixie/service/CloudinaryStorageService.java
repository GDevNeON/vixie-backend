package com.neong.vixie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Cloudinary-based content storage for creator uploads.
 * Uses the Cloudinary Upload API directly via HTTP (no SDK dependency).
 *
 * Requires environment variables:
 * - CLOUDINARY_CLOUD_NAME
 * - CLOUDINARY_API_KEY
 * - CLOUDINARY_API_SECRET
 */
@Service
@Slf4j
public class CloudinaryStorageService implements ContentStorageService {

    @Value("${cloudinary.cloud-name:#{null}}")
    private String cloudName;

    @Value("${cloudinary.api-key:#{null}}")
    private String apiKey;

    @Value("${cloudinary.api-secret:#{null}}")
    private String apiSecret;

    @Override
    public String upload(byte[] data, String folderPath, String fileName) {
        if (cloudName == null || apiKey == null || apiSecret == null) {
            log.warn("Cloudinary not configured — returning placeholder URL for {}/{}", folderPath, fileName);
            return "https://res.cloudinary.com/demo/raw/upload/v1/" + folderPath + fileName;
        }

        try {
            String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/raw/upload";
            String boundary = UUID.randomUUID().toString();
            String publicId = folderPath + fileName.replaceAll("\\.[^.]+$", "");

            URL url = new URL(uploadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(
                            (apiKey + ":" + apiSecret).getBytes(StandardCharsets.UTF_8)));

            try (OutputStream os = conn.getOutputStream()) {
                // api_key field
                writeMultipartField(os, boundary, "api_key", apiKey);
                // folder field
                writeMultipartField(os, boundary, "folder", folderPath);
                // public_id field
                writeMultipartField(os, boundary, "public_id", publicId);
                // file field
                writeMultipartFile(os, boundary, "file", fileName, data);
                // end boundary
                os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String responseBody = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                // Extract secure_url from JSON response (simple parsing)
                int secureUrlIdx = responseBody.indexOf("\"secure_url\"");
                if (secureUrlIdx >= 0) {
                    int start = responseBody.indexOf("\"", secureUrlIdx + 13) + 1;
                    int end = responseBody.indexOf("\"", start);
                    return responseBody.substring(start, end);
                }
                return "https://res.cloudinary.com/" + cloudName + "/raw/upload/" + publicId;
            } else {
                log.error("Cloudinary upload failed with status {}", responseCode);
                throw new IOException("Cloudinary upload failed: HTTP " + responseCode);
            }
        } catch (IOException e) {
            log.error("Failed to upload to Cloudinary: {}/{}", folderPath, fileName, e);
            throw new RuntimeException("Storage upload failed", e);
        }
    }

    @Override
    public void delete(String publicIdOrUrl) {
        log.info("Delete from Cloudinary requested: {}", publicIdOrUrl);
        // TODO: Implement Cloudinary destroy API call
    }

    private void writeMultipartField(OutputStream os, String boundary, String name, String value) throws IOException {
        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        os.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeMultipartFile(OutputStream os, String boundary, String fieldName, String fileName, byte[] data) throws IOException {
        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(data);
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
}
