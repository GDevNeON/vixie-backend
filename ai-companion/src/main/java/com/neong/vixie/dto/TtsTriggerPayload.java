package com.neong.vixie.dto;

/**
 * TTS trigger payload sent via STOMP to /user/queue/tts.
 * Flutter receives this after AI response completes and calls ElevenLabs directly.
 */
public record TtsTriggerPayload(
        String text,
        String voiceId,
        String token
) {
}
