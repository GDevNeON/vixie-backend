package com.neong.vixie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.dto.NotificationHistoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

    private static final String KEY_PREFIX = "vixie:user:";
    private static final String KEY_SUFFIX = ":notifications";
    private static final int MAX_HISTORY = 20;
    private static final Duration TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public void addNotification(String userId, NotificationHistoryItem item) {
        String key = buildKey(userId);
        redisTemplate.opsForList().leftPush(key, item);
        redisTemplate.opsForList().trim(key, 0, MAX_HISTORY - 1);
        redisTemplate.expire(key, TTL);
    }

    public List<NotificationHistoryItem> getHistory(String userId) {
        List<Object> raw = redisTemplate.opsForList().range(buildKey(userId), 0, -1);
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .map(this::toItem)
                .toList();
    }

    public void markAllAsRead(String userId) {
        String key = buildKey(userId);
        List<NotificationHistoryItem> readItems = getHistory(userId).stream()
                .map(NotificationHistoryItem::markRead)
                .toList();

        redisTemplate.delete(key);
        if (!readItems.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(key, readItems.toArray());
            redisTemplate.expire(key, TTL);
        }
    }

    private String buildKey(String userId) {
        return KEY_PREFIX + userId + KEY_SUFFIX;
    }

    private NotificationHistoryItem toItem(Object object) {
        if (object instanceof NotificationHistoryItem item) {
            return item;
        }
        return objectMapper.convertValue(object, NotificationHistoryItem.class);
    }
}
