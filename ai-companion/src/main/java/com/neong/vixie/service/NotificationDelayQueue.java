package com.neong.vixie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationDelayQueue {

    static final String QUEUE_KEY = "vixie:notification:queue";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public void enqueue(NotificationEvent event) {
        redisTemplate.opsForZSet().add(QUEUE_KEY, event, event.targetTimeEpoch());
    }

    public List<NotificationEvent> pollDueEvents() {
        long now = Instant.now().getEpochSecond();
        Set<Object> due = redisTemplate.opsForZSet().rangeByScore(QUEUE_KEY, 0, now);
        if (due == null || due.isEmpty()) {
            return List.of();
        }

        redisTemplate.opsForZSet().remove(QUEUE_KEY, due.toArray());
        return due.stream()
                .map(this::toEvent)
                .toList();
    }

    private NotificationEvent toEvent(Object object) {
        if (object instanceof NotificationEvent event) {
            return event;
        }
        return objectMapper.convertValue(object, NotificationEvent.class);
    }
}
