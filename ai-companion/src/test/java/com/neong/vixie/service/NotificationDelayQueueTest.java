package com.neong.vixie.service;

import com.neong.vixie.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDelayQueueTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ZSetOperations<String, Object> zSetOps;

    private NotificationDelayQueue queue;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        queue = new NotificationDelayQueue(redisTemplate);
    }

    @Test
    void enqueue_addsEventToSortedSetUsingTargetEpochScore() {
        NotificationEvent event = new NotificationEvent(
                "user_123", "char_default", NotificationEvent.MORNING_GREETING, 12345L, null);

        queue.enqueue(event);

        verify(zSetOps).add(NotificationDelayQueue.QUEUE_KEY, event, 12345L);
    }

    @Test
    void pollDueEvents_readsAndRemovesDueEvents() {
        NotificationEvent event = new NotificationEvent(
                "user_123", "char_default", NotificationEvent.FOCUS, 12345L, null);
        when(zSetOps.rangeByScore(eq(NotificationDelayQueue.QUEUE_KEY), eq(0.0), anyDouble()))
                .thenReturn(Set.of(event));

        List<NotificationEvent> result = queue.pollDueEvents();

        assertEquals(1, result.size());
        assertEquals(NotificationEvent.FOCUS, result.get(0).type());
        verify(zSetOps).remove(eq(NotificationDelayQueue.QUEUE_KEY), any(Object[].class));
    }
}
