package com.neong.vixie.dto;

import java.util.Map;

/**
 * Response DTO for user-specific character state (mood, relationship, personality settings).
 * Returned by GET /api/characters/{id}/state.
 */
public record CharacterStateResponse(
        String mood,
        Integer level,
        Integer currentXp,
        Integer xpToNextLevel,
        Map<String, Double> personalitySettings,
        String activeCharacterId
) {
}
