package com.neong.vixie.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for reading the current mood of a user-character pair from Redis.
 * Mood is updated by the batch analysis service (MoodAndXpBatchService).
 */
@Service
@RequiredArgsConstructor
public class MoodService {

    private static final String MOOD_KEY_PATTERN = "vixie:user:%s:mood:current";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Get the current mood for a user. Returns "NEUTRAL" if no mood is set.
     *
     * @param userId the user identifier
     * @return the current mood string (e.g., "HAPPY", "SAD", "NEUTRAL")
     */
    public String getCurrentMood(String userId) {
        String key = String.format(MOOD_KEY_PATTERN, userId);
        String mood = stringRedisTemplate.opsForValue().get(key);
        return mood != null ? mood : "NEUTRAL";
    }

    /**
     * Set the current mood for a user.
     *
     * @param userId the user identifier
     * @param mood   the mood value to set
     */
    public void setCurrentMood(String userId, String mood) {
        String key = String.format(MOOD_KEY_PATTERN, userId);
        stringRedisTemplate.opsForValue().set(key, mood);
    }
}
