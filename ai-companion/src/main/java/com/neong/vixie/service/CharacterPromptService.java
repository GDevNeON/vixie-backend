package com.neong.vixie.service;

import com.neong.vixie.model.CharacterEntity;
import com.neong.vixie.model.CharacterPersonality;
import com.neong.vixie.model.RelationshipState;
import com.neong.vixie.model.UserInteractionProfile;
import com.neong.vixie.repository.CharacterPersonalityRepository;
import com.neong.vixie.repository.CharacterRepository;
import com.neong.vixie.repository.RelationshipStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Builds character-specific system prompts for OpenAI conversations.
 *
 * Phase 4: Replaced stub with real DB lookups for character personality,
 * mood, and relationship state. Method signature stays stable.
 *
 * Phase 8: Added user preference personalization clause from
 * UserInteractionProfile (message length, topics, tone).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CharacterPromptService {

    private final CharacterRepository characterRepository;
    private final CharacterPersonalityRepository characterPersonalityRepository;
    private final RelationshipStateRepository relationshipStateRepository;
    private final MoodService moodService;
    private final UserInteractionProfileService profileService;

    /**
     * Build a system prompt for the given character.
     * Combines character personality, user's slider overrides, mood,
     * time-of-day context, and relationship tier.
     *
     * @param characterId the character identifier
     * @return the system prompt string
     */
    public String buildSystemPrompt(String userId, String characterId) {
        // Fetch character from DB (fall back to defaults if not found)
        CharacterEntity character = characterRepository.findById(characterId).orElse(null);
        String characterName = character != null ? character.getName() : "Hana";
        String characterDesc = character != null ? character.getDescription()
                : "a warm, playful, and caring AI companion";

        // Get personality — user override or character defaults
        String personalityBlend = buildPersonalityBlend(userId, characterId, character);

        // Get current mood from Redis
        String mood = moodService.getCurrentMood(userId);

        // Get relationship tier
        String relationshipTier = getRelationshipTier(userId, characterId);

        // Get time of day
        String timeOfDay = getTimeOfDayBucket();

        // Get user preference personalization (Phase 8)
        String personalization = buildPersonalizationClause(userId);

        return String.format(
                "You are %s, %s " +
                "Your personality settings: %s. " +
                "%s is currently feeling %s. Let this subtly colour her tone. " +
                "It is currently %s. " +
                "The user is a %s. " +
                "%s" +
                "Respond naturally and warmly in-character. " +
                "Keep responses concise but engaging.",
                characterName,
                characterDesc,
                personalityBlend,
                characterName,
                mood,
                timeOfDay,
                relationshipTier,
                personalization
        );
    }

    /**
     * Build personality blend text from user overrides or character defaults.
     */
    private String buildPersonalityBlend(String userId, String characterId, CharacterEntity character) {
        double seriousness, energy, gentleness;

        CharacterPersonality userPersonality = characterPersonalityRepository
                .findByUserIdAndCharacterId(userId, characterId)
                .orElse(null);

        if (userPersonality != null) {
            seriousness = userPersonality.getSeriousness();
            energy = userPersonality.getEnergy();
            gentleness = userPersonality.getGentleness();
        } else if (character != null) {
            seriousness = character.getDefaultSeriousness();
            energy = character.getDefaultEnergy();
            gentleness = character.getDefaultGentleness();
        } else {
            seriousness = 0.5;
            energy = 0.7;
            gentleness = 0.8;
        }

        return String.format("Seriousness: %.1f/1.0, Energy: %.1f/1.0, Gentleness: %.1f/1.0",
                seriousness, energy, gentleness);
    }

    /**
     * Map relationship level to human-readable tier label.
     */
    private String getRelationshipTier(String userId, String characterId) {
        int level = relationshipStateRepository
                .findByUserIdAndCharacterId(userId, characterId)
                .map(RelationshipState::getLevel)
                .orElse(1);

        return switch (level) {
            case 1, 2 -> "new friend";
            case 3, 4 -> "friend";
            case 5, 6 -> "close friend";
            case 7, 8 -> "best friend";
            case 9, 10 -> "soulmate";
            default -> "new friend";
        };
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

    /**
     * Build a personalization clause from the user's interaction profile.
     * Returns empty string if no profile exists yet (graceful fallback).
     */
    private String buildPersonalizationClause(String userId) {
        try {
            UserInteractionProfile profile = profileService.getProfile(userId);
            if (profile == null || profile.getTotalSessionCount() < 2) {
                return "";
            }

            String lengthPref;
            double avgLen = profile.getAvgMessageLength();
            if (avgLen < 10) lengthPref = "short";
            else if (avgLen < 30) lengthPref = "medium";
            else lengthPref = "long";

            String topics = profile.getTopTopics();
            // Strip JSON array brackets for readability
            topics = topics.replaceAll("[\\[\\]\"]", "");

            StringBuilder sb = new StringBuilder();
            sb.append(String.format(
                    "User preference: prefers %s responses, tone is %s. ",
                    lengthPref,
                    profile.getPreferredTone().toLowerCase()));

            if (!topics.isEmpty()) {
                sb.append(String.format("Topics they enjoy: %s. ", topics));
            }

            sb.append("Match their communication style. ");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to build personalization clause for user={}: {}", userId, e.getMessage());
            return "";
        }
    }
}
