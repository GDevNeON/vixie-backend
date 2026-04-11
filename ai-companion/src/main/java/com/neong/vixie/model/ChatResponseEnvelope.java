package com.neong.vixie.model;

/**
 * Outbound STOMP message payload sent to /user/queue/reply.
 *
 * Types:
 *   - "chunk" : streaming token fragment (text contains the token)
 *   - "done"  : stream complete (text is null)
 *   - "error" : error occurred (text contains error message)
 */
public record ChatResponseEnvelope(
        String type,
        String text
) {
    // Message type constants — avoid magic strings in controller
    public static final String TYPE_CHUNK = "chunk";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    public static ChatResponseEnvelope chunk(String text) {
        return new ChatResponseEnvelope(TYPE_CHUNK, text);
    }

    public static ChatResponseEnvelope done() {
        return new ChatResponseEnvelope(TYPE_DONE, null);
    }

    public static ChatResponseEnvelope error(String message) {
        return new ChatResponseEnvelope(TYPE_ERROR, message);
    }
}
