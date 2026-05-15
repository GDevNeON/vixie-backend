package com.neong.vixie.service;

import com.neong.vixie.model.CharacterEntity;
import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.model.RelationshipState;
import com.neong.vixie.repository.CharacterRepository;
import com.neong.vixie.repository.RelationshipStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Generates and caches a personalized daily greeting for each user.
 *
 * Phase 8: First daily interaction triggers an AI-generated greeting
 * based on character persona, time of day, and relationship tier.
 * Subsequent requests on the same day return the cached greeting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GreetingService {

    private final StringRedisTemplate stringRedisTemplate;
    private final OpenAiService openAiService;
    private final CharacterRepository characterRepository;
    private final RelationshipStateRepository relationshipStateRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Duration GREETING_TTL = Duration.ofHours(24);

    /**
     * Get or generate the daily greeting for a user + character pair.
     *
     * @return Map with "greeted" boolean and optional "message" string.
     *         If already greeted today, returns {"greeted": true}.
     *         If first time today, generates and returns {"greeted": false, "message": "..."}.
     */
    public Map<String, Object> getDailyGreeting(String userId, String characterId) {
        String today = LocalDate.now(ZoneOffset.UTC).format(DATE_FMT);
        String greetedKey = String.format("vixie:user:%s:greeted:%s", userId, today);
        String greetingKey = String.format("vixie:user:%s:greeting:%s", userId, today);

        // Check if already greeted today
        String cachedGreeting = stringRedisTemplate.opsForValue().get(greetingKey);
        if (cachedGreeting != null) {
            return Map.of("greeted", true, "message", cachedGreeting);
        }

        // Check greeted flag (in case greeting was generated but response was lost)
        Boolean alreadyGreeted = stringRedisTemplate.hasKey(greetedKey);
        if (Boolean.TRUE.equals(alreadyGreeted)) {
            return Map.of("greeted", true);
        }

        // Generate new greeting
        try {
            String greeting = generateGreeting(userId, characterId);

            // Cache greeting and set greeted flag
            stringRedisTemplate.opsForValue().set(greetingKey, greeting, GREETING_TTL);
            stringRedisTemplate.opsForValue().set(greetedKey, "true", GREETING_TTL);

            log.info("Generated daily greeting for user={} character={}", userId, characterId);
            return Map.of("greeted", false, "message", greeting);
        } catch (Exception e) {
            log.warn("Daily greeting generation failed for user={}: {}", userId, e.getMessage());

            // Fallback to template greeting
            String fallback = generateFallbackGreeting(userId, characterId);
            stringRedisTemplate.opsForValue().set(greetedKey, "true", GREETING_TTL);

            return Map.of("greeted", false, "message", fallback);
        }
    }

    private String generateGreeting(String userId, String characterId) {
        CharacterEntity character = characterRepository.findById(characterId).orElse(null);
        String characterName = character != null ? character.getName() : "Hana";
        String timeOfDay = getTimeOfDayBucket();
        String relationshipTier = getRelationshipTier(userId, characterId);

        String prompt = String.format(
                "You are %s. Generate a warm, brief greeting for the user for their " +
                "first interaction today. It is %s. Your relationship with them: %s. " +
                "Max 2 sentences. Stay in character. Be warm and personal.",
                characterName, timeOfDay, relationshipTier
        );

        return openAiService.callChat(prompt, List.of(
                ChatMessageDto.of("user", "Good " + timeOfDay + "!")
        ));
    }

    private String generateFallbackGreeting(String userId, String characterId) {
        CharacterEntity character = characterRepository.findById(characterId).orElse(null);
        String characterName = character != null ? character.getName() : "Hana";
        String timeOfDay = getTimeOfDayBucket();

        return String.format("Good %s! I'm glad to see you today~ How are you doing? 💕",
                timeOfDay);
    }

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

    private String getTimeOfDayBucket() {
        int hour = LocalTime.now(ZoneOffset.UTC).getHour();
        if (hour >= 6 && hour < 12) return "morning";
        if (hour >= 12 && hour < 17) return "afternoon";
        if (hour >= 17 && hour < 22) return "evening";
        return "night";
    }
}
