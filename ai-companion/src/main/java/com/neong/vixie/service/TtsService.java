package com.neong.vixie.service;

import com.neong.vixie.dto.TtsTriggerPayload;
import com.neong.vixie.model.CharacterEntity;
import com.neong.vixie.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for generating TTS trigger payloads using Azure TTS.
 * Provides Azure access tokens and voice configuration for client-side TTS calls.
 *
 * Architecture: Backend manages Azure subscription key and token issuance.
 * Flutter receives short-lived bearer tokens (10-min TTL) via STOMP trigger,
 * then calls Azure TTS REST API directly for minimal audio latency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TtsService {

    private final CharacterRepository characterRepository;
    private final AzureTtsService azureTtsService;

    /**
     * Generate a TTS trigger payload for the given text and character.
     * Returns null if the character has no voice configured or Azure is not set up.
     */
    public TtsTriggerPayload generateTtsPayload(String fullText, String characterId) {
        try {
            String token = azureTtsService.getAccessToken();
            CharacterEntity character = characterRepository.findById(characterId).orElse(null);

            if (character == null || character.getElevenlabsVoiceId() == null) {
                log.warn("Character {} has no voice configured — skipping TTS", characterId);
                return null;
            }

            return new TtsTriggerPayload(
                    fullText,
                    character.getElevenlabsVoiceId(),
                    token,
                    azureTtsService.getTokenExpiresAt(),
                    azureTtsService.getRegion()
            );
        } catch (Exception e) {
            log.warn("Azure TTS not available — skipping TTS trigger: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate a test voice payload for the given character (used by VoiceController).
     * Returns null if the character has no voice configured.
     */
    public TtsTriggerPayload generateTestPayload(String characterId) {
        String token = azureTtsService.getAccessToken();

        CharacterEntity character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        if (character.getElevenlabsVoiceId() == null) {
            throw new IllegalStateException("Character " + characterId + " has no voice configured");
        }

        return new TtsTriggerPayload(
                null, // No text for test — frontend provides its own sample text
                character.getElevenlabsVoiceId(),
                token,
                azureTtsService.getTokenExpiresAt(),
                azureTtsService.getRegion()
        );
    }
}
