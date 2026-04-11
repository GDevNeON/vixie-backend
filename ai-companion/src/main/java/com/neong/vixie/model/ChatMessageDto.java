package com.neong.vixie.model;

import java.time.Instant;

/**
 * DTO representing a single message in a conversation history.
 * Stored as JSON in Redis lists.
 *
 * @param role      "user" or "assistant" (or "system" for summarization)
 * @param content   the message text
 * @param timestamp ISO-8601 timestamp of when the message was created
 */
public record ChatMessageDto(
        String role,
        String content,
        String timestamp
) {
    /**
     * Factory: create a new ChatMessageDto with the current timestamp.
     */
    public static ChatMessageDto of(String role, String content) {
        return new ChatMessageDto(role, content, Instant.now().toString());
    }
}
