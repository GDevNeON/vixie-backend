package com.neong.vixie.service;

import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Builds character-specific system prompts for OpenAI conversations.
 *
 * Phase 2: Uses a hardcoded stub character. Phase 4 will replace the
 * internals with a database lookup while keeping this interface stable.
 */
@Service
public class CharacterPromptService {

    // TODO Phase 4: replace with DB lookup
    private static final String DEFAULT_CHARACTER_NAME = "Hana";
    private static final String DEFAULT_CHARACTER_PERSONALITY =
            "a warm, playful, and caring AI companion who loves chatting " +
            "about everyday life. She uses casual language and occasionally " +
            "adds emotional expressions or gentle humor to her responses.";

    /**
     * Build a system prompt for the given character.
     * Combines character personality, time-of-day context, and relationship hint.
     *
     * @param characterId the character identifier (ignored in Phase 2 — uses stub)
     * @return the system prompt string
     */
    public String buildSystemPrompt(String characterId) {
        String timeOfDay = getTimeOfDayBucket();
        String relationshipHint = "new friend"; // TODO Phase 4: from RelationshipService

        return String.format(
                "You are %s, %s " +
                "It is currently %s. " +
                "The user is a %s. " +
                "Respond naturally and warmly in-character. " +
                "Keep responses concise but engaging.",
                DEFAULT_CHARACTER_NAME,
                DEFAULT_CHARACTER_PERSONALITY,
                timeOfDay,
                relationshipHint
        );
    }

    /**
     * Determine the time-of-day bucket based on current UTC hour.
     * morning: 6-12, afternoon: 12-17, evening: 17-22, night: 22-6
     */
    private String getTimeOfDayBucket() {
        int hour = LocalTime.now(ZoneOffset.UTC).getHour();
        if (hour >= 6 && hour < 12) return "morning";
        if (hour >= 12 && hour < 17) return "afternoon";
        if (hour >= 17 && hour < 22) return "evening";
        return "night";
    }
}
