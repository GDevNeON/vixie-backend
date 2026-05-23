package com.neong.vixie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationDelayQueue {

    static final String QUEUE_KEY = "vixie:notification:queue";
    private static final DefaultRedisScript<Object> POP_DUE_EVENT_SCRIPT = new DefaultRedisScript<>("""
            local item = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, 1)
            if #item == 0 then
              return nil
            end
            redis.call('ZREM', KEYS[1], item[1])
            return item[1]
            """, Object.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void enqueue(NotificationEvent event) {
        redisTemplate.opsForZSet().add(QUEUE_KEY, event, event.targetTimeEpoch());
    }

    public List<NotificationEvent> pollDueEvents() {
        long now = Instant.now().getEpochSecond();
        List<NotificationEvent> events = new ArrayList<>();
        Object due;
        do {
            due = redisTemplate.execute(POP_DUE_EVENT_SCRIPT, Collections.singletonList(QUEUE_KEY), now);
            if (due != null) {
                events.add(toEvent(due));
            }
        } while (due != null);
        return events;
    }

    private NotificationEvent toEvent(Object object) {
        if (object instanceof NotificationEvent event) {
            return event;
        }
        return objectMapper.convertValue(object, NotificationEvent.class);
    }
}
