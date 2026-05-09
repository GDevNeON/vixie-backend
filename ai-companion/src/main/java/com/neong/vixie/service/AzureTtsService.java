package com.neong.vixie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Azure Cognitive Services TTS token management.
 * Caches the short-lived access token (10-minute TTL) and refreshes at 9 minutes.
 *
 * The token is used by Flutter to call Azure TTS REST API directly,
 * keeping the subscription key server-side.
 */
@Service
@Slf4j
public class AzureTtsService {

    @Value("${ai.azure.speech.key:}")
    private String azureSpeechKey;

    @Value("${ai.azure.speech.region:eastasia}")
    private String azureSpeechRegion;

    private String cachedToken;
    private Instant tokenExpiry = Instant.MIN;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Get a valid Azure access token, refreshing if needed.
     * Token TTL is 10 minutes; we refresh at 9 minutes to avoid edge-case expiry.
     */
    public synchronized String getAccessToken() {
        if (azureSpeechKey == null || azureSpeechKey.isBlank()) {
            throw new IllegalStateException("Azure Speech key not configured (AZURE_SPEECH_KEY)");
        }

        // Refresh if token expired or within 60 seconds of expiry
        if (cachedToken == null || Instant.now().isAfter(tokenExpiry.minusSeconds(60))) {
            refreshToken();
        }

        return cachedToken;
    }

    /**
     * Get the configured Azure Speech region.
     */
    public String getRegion() {
        return azureSpeechRegion;
    }

    /**
     * Get the token expiry timestamp (ISO-8601 string).
     */
    public String getTokenExpiresAt() {
        return tokenExpiry.toString();
    }

    private void refreshToken() {
        String issueTokenUrl = String.format(
                "https://%s.api.cognitive.microsoft.com/sts/v1.0/issueToken",
                azureSpeechRegion
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(issueTokenUrl))
                    .header("Ocp-Apim-Subscription-Key", azureSpeechKey)
                    .header("Content-Length", "0")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Azure issueToken failed with status {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Azure issueToken failed: HTTP " + response.statusCode());
            }

            cachedToken = response.body();
            tokenExpiry = Instant.now().plusSeconds(600); // 10 minutes
            log.info("Azure TTS token refreshed, expires at {}", tokenExpiry);
        } catch (IOException | InterruptedException e) {
            log.error("Failed to fetch Azure TTS token: {}", e.getMessage());
            throw new RuntimeException("Azure TTS token fetch failed", e);
        }
    }
}
