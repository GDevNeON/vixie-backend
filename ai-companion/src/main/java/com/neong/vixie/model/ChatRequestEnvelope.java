package com.neong.vixie.model;

/**
 * Inbound STOMP message payload from Flutter client.
 * Sent to /app/chat destination.
 *
 * @param characterId the character to chat with (e.g., "char_default")
 * @param message     the user's message text
 */
public record ChatRequestEnvelope(
        String characterId,
        String message
) {
}
