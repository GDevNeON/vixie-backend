package com.neong.vixie.dto;

/**
 * Request DTO for POST /api/users/preferences/active-character.
 */
public record ActiveCharacterRequest(
        String characterId
) {
}
