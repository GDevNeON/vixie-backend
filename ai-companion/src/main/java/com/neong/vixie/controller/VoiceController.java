package com.neong.vixie.controller;

import com.neong.vixie.dto.TtsTriggerPayload;
import com.neong.vixie.service.TtsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for voice-related endpoints.
 * - GET /api/characters/{id}/voice/test — get test voice payload (voiceId + token)
 */
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class VoiceController {

    private final TtsService ttsService;

    /**
     * Returns the voice configuration for testing a character's voice.
     * Frontend uses this to call ElevenLabs directly with a sample phrase.
     */
    @GetMapping("/{id}/voice/test")
    public ResponseEntity<TtsTriggerPayload> testVoice(@PathVariable String id) {
        TtsTriggerPayload payload = ttsService.generateTestPayload(id);
        return ResponseEntity.ok(payload);
    }
}
