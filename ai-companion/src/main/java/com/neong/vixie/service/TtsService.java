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

        return new TtsTriggerPayload(fullText);
    }

    /**
     * Proxy TTS request to ElevenLabs stream endpoint.
     */
    public org.springframework.http.ResponseEntity<String> streamTts(String characterId, String text) {
        if (elevenlabsApiKey == null || elevenlabsApiKey.isBlank()) {
            throw new IllegalStateException("ElevenLabs API key not configured");
        }

        CharacterEntity character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        if (character.getElevenlabsVoiceId() == null) {
            throw new IllegalStateException("Character " + characterId + " has no voice configured");
        }

        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.set("xi-api-key", elevenlabsApiKey);
        
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("text", text);
        body.put("model_id", "eleven_multilingual_v2");
        java.util.Map<String, Object> voiceSettings = new java.util.HashMap<>();
        voiceSettings.put("stability", 0.5);
        voiceSettings.put("similarity_boost", 0.75);
        body.put("voice_settings", voiceSettings);
        
        org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(body, headers);
        
        String url = "https://api.elevenlabs.io/v1/text-to-speech/" + character.getElevenlabsVoiceId() + "/stream/with-timestamps";
        
        try {
            return restTemplate.postForEntity(url, entity, String.class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("ElevenLabs API returned error: {}", e.getResponseBodyAsString());
            return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
