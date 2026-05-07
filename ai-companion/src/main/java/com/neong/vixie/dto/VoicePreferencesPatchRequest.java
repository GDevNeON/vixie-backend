package com.neong.vixie.dto;

/**
 * Request DTO for patching voice preferences.
 * Supports partial updates — null fields are skipped.
 */
public record VoicePreferencesPatchRequest(
        Boolean voiceMuted,
        Double voiceVolume
) {
}
