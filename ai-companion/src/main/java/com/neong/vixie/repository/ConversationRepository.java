package com.neong.vixie.repository;

import com.neong.vixie.model.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

/**
 * Redis-backed repository for conversation history.
 * Key schema: vixie:chat:{userId}:{characterId}:history
 *
 * - Messages stored as a Redis List (JSON-serialized ChatMessageDto)
 * - Capped at 20 messages (oldest trimmed on push)
 * - 48-hour TTL reset on every write
 */
@Repository
@RequiredArgsConstructor
public class ConversationRepository {

    private static final String KEY_PREFIX = "vixie:chat:";
    private static final String KEY_SUFFIX = ":history";
    private static final int MAX_HISTORY = 20;
    private static final Duration TTL = Duration.ofHours(48);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Build the Redis key for a user+character conversation.
     */
    private String buildKey(String userId, String characterId) {
        return KEY_PREFIX + userId + ":" + characterId + KEY_SUFFIX;
    }

    /**
     * Add a message to the conversation history.
     * Trims to last 20 messages and resets 48h TTL.
     */
    public void addMessage(String userId, String characterId, ChatMessageDto msg) {
        String key = buildKey(userId, characterId);
        redisTemplate.opsForList().rightPush(key, msg);
        redisTemplate.opsForList().trim(key, -MAX_HISTORY, -1);
        redisTemplate.expire(key, TTL);
    }

    /**
     * Get the full conversation history for a user+character pair.
     */
    @SuppressWarnings("unchecked")
    public List<ChatMessageDto> getHistory(String userId, String characterId) {
        String key = buildKey(userId, characterId);
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .map(obj -> (ChatMessageDto) obj)
                .toList();
    }

    /**
     * Get the current size of the conversation history.
     */
    public long getHistorySize(String userId, String characterId) {
        String key = buildKey(userId, characterId);
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0;
    }

    /**
     * Replace the oldest N messages with a single summary message.
     * Used by SummarizationService when history exceeds threshold.
     *
     * @param oldestCount number of oldest messages to extract and replace
     * @param summaryDto  the summary message to insert at the beginning
     */
    public void replaceOldestWithSummary(String userId, String characterId,
                                          int oldestCount, ChatMessageDto summaryDto) {
        String key = buildKey(userId, characterId);
        // Trim away the oldest N messages
        redisTemplate.opsForList().trim(key, oldestCount, -1);
        // Prepend the summary at the beginning
        redisTemplate.opsForList().leftPush(key, summaryDto);
        // Reset TTL
        redisTemplate.expire(key, TTL);
    }
}
