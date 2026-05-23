package com.neong.vixie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.dto.NotificationHistoryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationHistoryServiceTest {

    private static final String USER_ID = "user_123";
    private static final String KEY = "vixie:user:user_123:notifications";
    private static final String READ_KEY = "vixie:user:user_123:notifications:read";

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ListOperations<String, Object> listOps;
    @Mock private SetOperations<String, Object> setOps;

    private NotificationHistoryService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        service = new NotificationHistoryService(redisTemplate, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void addNotification_leftPushesTrimsTo20AndSetsSevenDayTtl() {
        NotificationHistoryItem item = sampleItem(false);

        service.addNotification(USER_ID, item);

        verify(listOps).leftPush(KEY, item);
        verify(listOps).trim(KEY, 0, 19);
        verify(redisTemplate).expire(KEY, Duration.ofDays(7));
    }

    @Test
    void getHistory_deserializesItemsAndReturnsEmptyForMissingList() {
        Instant sentAt = Instant.parse("2026-05-22T00:00:00Z");
        Map<String, Object> raw = Map.of(
                "id", "notif_1",
                "type", "MORNING_GREETING",
                "title", "Morning",
                "body", "Hello",
                "characterId", "char_default",
                "sentAt", sentAt.toString(),
                "isRead", false
        );
        when(listOps.range(KEY, 0, -1)).thenReturn(List.of(raw));
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(READ_KEY)).thenReturn(Set.of("notif_1"));

        List<NotificationHistoryItem> result = service.getHistory(USER_ID);

        assertEquals(1, result.size());
        assertEquals("notif_1", result.get(0).id());
        assertTrue(result.get(0).isRead());
        assertEquals(sentAt, result.get(0).sentAt());

        when(listOps.range(KEY, 0, -1)).thenReturn(null);
        assertTrue(service.getHistory(USER_ID).isEmpty());
    }

    @Test
    void markAllAsRead_tracksReadIdsWithoutRewritingHistoryList() {
        when(listOps.range(KEY, 0, -1)).thenReturn(List.of(sampleItem("notif_1", false), sampleItem("notif_2", false)));
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(READ_KEY)).thenReturn(Set.of());

        service.markAllAsRead(USER_ID);

        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(setOps).add(eq(READ_KEY), captor.capture());
        Object[] stored = captor.getValue();
        assertEquals(2, stored.length);
        assertEquals("notif_1", stored[0]);
        assertEquals("notif_2", stored[1]);
        verify(redisTemplate).expire(READ_KEY, Duration.ofDays(7));
        verify(redisTemplate, never()).delete(KEY);
        verify(listOps, never()).rightPushAll(anyString(), any(Object[].class));
    }

    private NotificationHistoryItem sampleItem(boolean isRead) {
        return sampleItem("notif_1", isRead);
    }

    private NotificationHistoryItem sampleItem(String id, boolean isRead) {
        return new NotificationHistoryItem(
                id, "MORNING_GREETING", "Morning", "Hello",
                "char_default", Instant.parse("2026-05-22T00:00:00Z"), isRead
        );
    }
}
