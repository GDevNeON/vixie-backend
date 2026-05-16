package com.neong.vixie.service;

import com.neong.vixie.model.ChatMessageDto;
import com.neong.vixie.model.RelationshipState;
import com.neong.vixie.repository.RelationshipStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Batch service that analyzes conversation sentiment to update mood and relationship XP.
 * Called asynchronously on every message from ChatController — does NOT block the stream.
 *
 * Phase 8: Also emits emotion events via STOMP to /user/queue/emotion so the Flutter
 * client can drive Live2D facial expressions in real-time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MoodAndXpBatchService {

    private final GeminiService geminiService;
    private final MoodService moodService;
    private final RelationshipStateRepository relationshipStateRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String ANALYSIS_PROMPT =
            "Analyze the tone of these recent messages. " +
            "Respond ONLY in JSON: { \"mood\": \"HAPPY|SAD|NEUTRAL|ENERGETIC|TIRED|ANXIOUS\", \"xpDelta\": <integer between 0 and 15> }. " +
            "Be highly sensitive! Avoid NEUTRAL. If positive/agreeing, use HAPPY or ENERGETIC. If asking questions, use ENERGETIC or ANXIOUS. " +
            "xpDelta should be 5-15 for positive conversations, 2 for neutral, 0 for negative. " +
            "No other text.";

    private static final Pattern MOOD_PATTERN = Pattern.compile("(?i)\"?mood\"?\\s*:\\s*\"([A-Za-z]+)\"");
    private static final Pattern XP_PATTERN = Pattern.compile("(?i)\"?xpDelta\"?\\s*:\\s*(\\d+)");

    /**
     * Map mood strings to Live2D expression IDs.
     * Backend owns this mapping so Flutter doesn't need a local lookup table.
     */
    private static final Map<String, String> MOOD_TO_EXPRESSION = Map.of(
            "HAPPY", "ExpressionHappy",
            "SAD", "ExpressionSad",
            "NEUTRAL", "ExpressionNeutral",
            "ENERGETIC", "ExpressionSurprised",
            "TIRED", "ExpressionSad",
            "ANXIOUS", "ExpressionSad"
    );

    /**
     * Asynchronously analyze the batch of recent messages and update mood + XP.
     * This method MUST NOT block the caller (ChatController streaming).
     */
    @Async
    public void analyzeAndApplyBatch(String userId, String characterId, List<ChatMessageDto> recentMessages) {
        try {
            // Format the history into a single text block so the LLM doesn't treat it as a multi-turn continuation
            StringBuilder historyText = new StringBuilder();
            for (ChatMessageDto msg : recentMessages) {
                historyText.append(msg.role().toUpperCase()).append(": ").append(msg.content()).append("\n");
            }
            List<ChatMessageDto> promptMessages = List.of(
                    ChatMessageDto.of("user", historyText.toString())
            );

            // Call LLM for analysis
            String response = geminiService.callChat(ANALYSIS_PROMPT, promptMessages);
            log.info("Gemini raw mood response: {}", response);

            // Parse mood
            String mood = "NEUTRAL";
            Matcher moodMatcher = MOOD_PATTERN.matcher(response);
            if (moodMatcher.find()) {
                mood = moodMatcher.group(1).toUpperCase();
            }

            // Parse XP delta
            int xpDelta = 2;
            Matcher xpMatcher = XP_PATTERN.matcher(response);
            if (xpMatcher.find()) {
                xpDelta = Integer.parseInt(xpMatcher.group(1));
                xpDelta = Math.min(15, Math.max(0, xpDelta)); // Clamp 0-15
            }

            // Check if mood actually changed before emitting (prevents expression flickering)
            String previousMood = moodService.getCurrentMood(userId);

            // Update mood in Redis
            moodService.setCurrentMood(userId, mood);
            log.info("Batch analysis: mood={} xpDelta={} for user={}", mood, xpDelta, userId);

            // Emit STOMP emotion event (Frontend handles deduplication)
            emitEmotionEvent(userId, mood);

            // Update relationship XP in Postgres
            updateRelationshipXp(userId, characterId, xpDelta);

        } catch (Exception e) {
            // On failure, keep existing mood/XP — log and continue
            log.error("Batch mood+XP analysis failed for user={}: {}", userId, e.getMessage());
        }
    }

    /**
     * Push emotion update to Flutter via STOMP /user/queue/emotion.
     * Resolves mood to expressionId server-side. Catches all messaging errors
     * so they never block the async batch process.
     */
    private void emitEmotionEvent(String userId, String mood) {
        try {
            String expressionId = MOOD_TO_EXPRESSION.getOrDefault(mood, "ExpressionNeutral");
            Map<String, Object> payload = Map.of(
                    "mood", mood,
                    "expressionId", expressionId
            );
            messagingTemplate.convertAndSendToUser(userId, "/queue/emotion", payload);
            log.info("Emotion event emitted: mood={} expressionId={} for user={}", mood, expressionId, userId);
        } catch (Exception e) {
            log.warn("Failed to emit emotion event for user={}: {}", userId, e.getMessage());
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
