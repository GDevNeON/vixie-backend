package com.neong.vixie.controller;


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
     * Proxy TTS stream from ElevenLabs to the client.
     */
    @org.springframework.web.bind.annotation.PostMapping("/{id}/voice/stream")
    public ResponseEntity<String> streamVoice(@PathVariable String id, @org.springframework.web.bind.annotation.RequestBody com.neong.vixie.dto.TtsStreamRequest request) {
        return ttsService.streamTts(id, request.text());
    }
}
