package com.neong.vixie.service;

import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.model.RelationshipState;
import com.neong.vixie.repository.RelationshipStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Batch service that analyzes conversation sentiment to update mood and relationship XP.
 * Called asynchronously every 5 messages from ChatController — does NOT block the stream.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoodAndXpBatchService {

    private final OpenAiService openAiService;
    private final MoodService moodService;
    private final RelationshipStateRepository relationshipStateRepository;

    private static final String ANALYSIS_PROMPT =
            "Analyze the tone of these recent messages. " +
            "Respond ONLY in JSON: { \"mood\": \"HAPPY|SAD|NEUTRAL|ENERGETIC|TIRED|ANXIOUS\", \"xpDelta\": <integer between 0 and 15> }. " +
            "xpDelta should be 5-15 for positive conversations, 2 for neutral, 0 for negative. " +
            "No other text.";

    private static final Pattern MOOD_PATTERN = Pattern.compile("\"mood\"\\s*:\\s*\"(\\w+)\"");
    private static final Pattern XP_PATTERN = Pattern.compile("\"xpDelta\"\\s*:\\s*(\\d+)");

    /**
     * Asynchronously analyze the batch of recent messages and update mood + XP.
     * This method MUST NOT block the caller (ChatController streaming).
     */
    @Async
    public void analyzeAndApplyBatch(String userId, String characterId, List<ChatMessageDto> recentMessages) {
        try {
            log.info("Starting batch mood+XP analysis for user={} character={}", userId, characterId);

            // Call LLM for analysis
            String response = openAiService.callChat(ANALYSIS_PROMPT, recentMessages);

            // Parse mood
            String mood = "NEUTRAL";
            Matcher moodMatcher = MOOD_PATTERN.matcher(response);
            if (moodMatcher.find()) {
                mood = moodMatcher.group(1);
            }

            // Parse XP delta
            int xpDelta = 2;
            Matcher xpMatcher = XP_PATTERN.matcher(response);
            if (xpMatcher.find()) {
                xpDelta = Integer.parseInt(xpMatcher.group(1));
                xpDelta = Math.min(15, Math.max(0, xpDelta)); // Clamp 0-15
            }

            // Update mood in Redis
            moodService.setCurrentMood(userId, mood);
            log.info("Batch analysis: mood={} xpDelta={} for user={}", mood, xpDelta, userId);

            // Update relationship XP in Postgres
            updateRelationshipXp(userId, characterId, xpDelta);

        } catch (Exception e) {
            // On failure, keep existing mood/XP — log and continue
            log.error("Batch mood+XP analysis failed for user={}: {}", userId, e.getMessage());
        }
    }

    private void updateRelationshipXp(String userId, String characterId, int xpDelta) {
        if (xpDelta <= 0) return;

        RelationshipState state = relationshipStateRepository
                .findByUserIdAndCharacterId(userId, characterId)
                .orElseGet(() -> {
                    RelationshipState newState = RelationshipState.builder()
                            .id(UUID.randomUUID().toString())
                            .userId(userId)
                            .characterId(characterId)
                            .level(1)
                            .currentXp(0)
                            .xpToNextLevel(100)
                            .build();
                    return relationshipStateRepository.save(newState);
                });

        int newXp = state.getCurrentXp() + xpDelta;

        // Level up check — level 10 is max
        if (newXp >= state.getXpToNextLevel() && state.getLevel() < 10) {
            int newLevel = state.getLevel() + 1;
            int overflow = newXp - state.getXpToNextLevel();
            state.setLevel(newLevel);
            state.setCurrentXp(overflow);
            state.setXpToNextLevel(newLevel * 100);
            log.info("Level up! user={} character={} level={}", userId, characterId, newLevel);
        } else {
            state.setCurrentXp(Math.min(newXp, state.getXpToNextLevel()));
        }

        relationshipStateRepository.save(state);
    }
}
