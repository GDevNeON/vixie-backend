package com.neong.vixie.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for calling user-auth internal APIs.
 * Uses X-Service-Key header for authentication.
 */
@Service
public class UserAuthClient {

    private static final Logger log = LoggerFactory.getLogger(UserAuthClient.class);

    private final RestClient restClient;
    private final String serviceKey;

    public UserAuthClient(
            @Value("${vixie.user-auth.base-url:http://localhost:8080}") String baseUrl,
            @Value("${vixie.internal.service-key:}") String serviceKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.serviceKey = serviceKey;
    }

    /**
     * Get pity count for user+banner from user-auth.
     */
    public int getPityCount(String userId, String bannerId) {
        try {
            Map<String, Integer> response = restClient.get()
                    .uri("/api/internal/gacha/pity?userId={userId}&bannerId={bannerId}", userId, bannerId)
                    .header("X-Service-Key", serviceKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return response != null ? response.getOrDefault("current_pity", 0) : 0;
        } catch (Exception e) {
            log.error("Failed to get pity count: userId={}, bannerId={}", userId, bannerId, e);
            return 0; // Default to 0 on error — conservative (no free pity)
        }
    }

    /**
     * Commit pull results to user-auth (deduct coins, add inventory, update pity).
     * @return new wallet balance
     */
    @SuppressWarnings("unchecked")
    public int commitPulls(String userId, String bannerId, int totalCost,
                           List<Map<String, String>> pullResults) {
        Map<String, Object> request = Map.of(
                "userId", userId,
                "bannerId", bannerId,
                "totalCost", totalCost,
                "pullResults", pullResults
        );

        Map<String, Object> response = restClient.post()
                .uri("/api/internal/gacha/commit")
                .header("X-Service-Key", serviceKey)
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            throw new RuntimeException("Gacha commit failed: " + response);
        }

        return ((Number) response.get("new_balance")).intValue();
    }
}
