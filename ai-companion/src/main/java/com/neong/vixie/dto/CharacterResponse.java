package com.neong.vixie.dto;

import java.util.Map;

/**
 * Response DTO for character info returned by GET /api/characters and GET /api/characters/{id}.
 */
public record CharacterResponse(
        String id,
        String name,
        String description,
        String avatarUrl,
        Map<String, Double> defaultPersonality
) {
}
