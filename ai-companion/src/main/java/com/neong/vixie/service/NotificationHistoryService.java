package com.neong.vixie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.dto.NotificationHistoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

    private static final String KEY_PREFIX = "vixie:user:";
    private static final String KEY_SUFFIX = ":notifications";
    private static final String READ_KEY_SUFFIX = ":notifications:read";
    private static final int MAX_HISTORY = 20;
    private static final Duration TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void addNotification(String userId, NotificationHistoryItem item) {
        String key = buildKey(userId);
        redisTemplate.opsForList().leftPush(key, item);
        redisTemplate.opsForList().trim(key, 0, MAX_HISTORY - 1);
        redisTemplate.expire(key, TTL);
    }

    public List<NotificationHistoryItem> getHistory(String userId) {
        String key = buildKey(userId);
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) {
            return List.of();
        }
        Set<String> readIds = readIds(userId);
        return raw.stream()
                .map(this::toItem)
                .map(item -> readIds.contains(item.id()) ? item.markRead() : item)
                .toList();
    }

    public void markAllAsRead(String userId) {
        List<String> ids = getHistory(userId).stream()
                .map(NotificationHistoryItem::id)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        if (!ids.isEmpty()) {
            String readKey = buildReadKey(userId);
            redisTemplate.opsForSet().add(readKey, ids.toArray());
            redisTemplate.expire(readKey, TTL);
        }
    }

    private String buildKey(String userId) {
        return KEY_PREFIX + userId + KEY_SUFFIX;
    }

    private String buildReadKey(String userId) {
        return KEY_PREFIX + userId + READ_KEY_SUFFIX;
    }

    private Set<String> readIds(String userId) {
        Set<Object> raw = redisTemplate.opsForSet().members(buildReadKey(userId));
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        return raw.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private NotificationHistoryItem toItem(Object object) {
        if (object instanceof NotificationHistoryItem item) {
            return item;
        }
        return objectMapper.convertValue(object, NotificationHistoryItem.class);
    }
}
