package com.neong.vixie.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for voice preferences — used by GET/PATCH /api/users/preferences/voice.
 */
public record VoicePreferencesDto(
        @JsonProperty("voice_muted") Boolean voiceMuted,
        @JsonProperty("voice_volume") Double voiceVolume
) {
}
