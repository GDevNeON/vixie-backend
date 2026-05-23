package com.neong.vixie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.dto.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
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
        queue = new NotificationDelayQueue(redisTemplate, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void enqueue_addsEventToSortedSetUsingTargetEpochScore() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        NotificationEvent event = new NotificationEvent(
                "user_123", "char_default", NotificationEvent.MORNING_GREETING, 12345L, null);

        queue.enqueue(event);

        verify(zSetOps).add(NotificationDelayQueue.QUEUE_KEY, event, 12345L);
    }

    @Test
    void pollDueEvents_readsAndRemovesDueEvents() {
        NotificationEvent event = new NotificationEvent(
                "user_123", "char_default", NotificationEvent.FOCUS, 12345L, null);
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of(NotificationDelayQueue.QUEUE_KEY)), anyLong()))
                .thenReturn(event)
                .thenReturn(null);

        List<NotificationEvent> result = queue.pollDueEvents();

        assertEquals(1, result.size());
        assertEquals(NotificationEvent.FOCUS, result.get(0).type());
        verify(redisTemplate, times(2)).execute(
                any(RedisScript.class),
                eq(List.of(NotificationDelayQueue.QUEUE_KEY)),
                anyLong());
    }
}
