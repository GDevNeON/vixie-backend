package com.neong.vixie.service;

import com.neong.vixie.dto.TtsTriggerPayload;
import com.neong.vixie.model.CharacterEntity;
import com.neong.vixie.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for generating TTS trigger payloads.
 * Provides the ElevenLabs voice ID and API token for client-side TTS calls.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TtsService {

    private final CharacterRepository characterRepository;

    @Value("${ai.elevenlabs.api-key:}")
    private String elevenlabsApiKey;

    /**
     * Generate a TTS trigger payload for the given text and character.
     * Returns null if the character has no voice configured or no API key is set.
     */
    public TtsTriggerPayload generateTtsPayload(String fullText, String characterId) {
        if (elevenlabsApiKey == null || elevenlabsApiKey.isBlank()) {
            log.warn("ElevenLabs API key not configured — skipping TTS trigger");
            return null;
        }

        CharacterEntity character = characterRepository.findById(characterId).orElse(null);
        if (character == null || character.getElevenlabsVoiceId() == null) {
            log.warn("Character {} has no ElevenLabs voice configured — skipping TTS", characterId);
            return null;
        }

        return new TtsTriggerPayload(
                fullText,
                character.getElevenlabsVoiceId(),
                elevenlabsApiKey
        );
    }

    /**
     * Generate a test voice payload for the given character (used by VoiceController).
     * Returns null if the character has no voice configured.
     */
    public TtsTriggerPayload generateTestPayload(String characterId) {
        if (elevenlabsApiKey == null || elevenlabsApiKey.isBlank()) {
            throw new IllegalStateException("ElevenLabs API key not configured");
        }

        CharacterEntity character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        if (character.getElevenlabsVoiceId() == null) {
            throw new IllegalStateException("Character " + characterId + " has no voice configured");
        }

        return new TtsTriggerPayload(
                null, // No text for test — frontend provides its own sample text
                character.getElevenlabsVoiceId(),
                elevenlabsApiKey
        );
    }
}
