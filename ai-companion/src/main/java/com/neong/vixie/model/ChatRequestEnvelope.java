package com.neong.vixie.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Inbound STOMP message payload from Flutter client.
 * Sent to /app/chat destination.
 *
 * @param characterId the character to chat with (e.g., "char_default")
 * @param message     the user's message text
 */
public record ChatRequestEnvelope(
        @JsonProperty("characterId") String characterId,
        @JsonProperty("message") String message
) {
}
