package com.neong.vixie.dto;

/**
 * Request DTO for PATCH /api/characters/{id}/personality.
 * All values are 0.0 - 1.0 slider range.
 */
public record PersonalityPatchRequest(
        Double seriousness,
        Double energy,
        Double gentleness
) {
}
