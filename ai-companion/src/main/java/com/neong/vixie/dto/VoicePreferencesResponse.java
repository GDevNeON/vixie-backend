package com.neong.vixie.dto;

/**
 * Response DTO for voice preferences.
 */
public record VoicePreferencesResponse(
        Boolean voiceMuted,
        Double voiceVolume
) {
}
