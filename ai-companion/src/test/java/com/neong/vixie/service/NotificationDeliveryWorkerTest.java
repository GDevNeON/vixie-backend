package com.neong.vixie.service;

import com.neong.vixie.dto.NotificationEvent;
import com.neong.vixie.dto.NotificationHistoryItem;
import com.neong.vixie.model.NotificationPreferences;
import com.neong.vixie.repository.NotificationPreferencesRepository;
import com.neong.vixie.repository.NotificationTokenRepository;
import com.neong.vixie.repository.UserOccasionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryWorkerTest {

    @Mock private NotificationDelayQueue notificationDelayQueue;
    @Mock private NotificationHistoryService notificationHistoryService;
    @Mock private NotificationTokenRepository notificationTokenRepository;
    @Mock private NotificationPreferencesRepository preferencesRepository;
    @Mock private UserOccasionRepository userOccasionRepository;
    @Mock private GreetingService greetingService;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private NotificationDeliveryWorker worker;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        worker = new NotificationDeliveryWorker(
                notificationDelayQueue,
                notificationHistoryService,
                notificationTokenRepository,
                preferencesRepository,
                userOccasionRepository,
                greetingService,
                stringRedisTemplate
        );
    }

    @Test
    void deliverDueNotifications_recordsHistoryAndRequeuesDailyEvent() {
        NotificationEvent event = new NotificationEvent(
                "user_123", "char_default", NotificationEvent.MORNING_GREETING, 12345L, null);
        NotificationPreferences preferences = NotificationPreferences.builder()
                .userId("user_123")
                .characterId("char_default")
                .greetingEnabled(true)
                .greetingTime(LocalTime.of(8, 0))
                .timezone("UTC")
                .build();

        when(notificationDelayQueue.pollDueEvents()).thenReturn(List.of(event));
        when(valueOps.get("vixie:user:user_123:pregeneratedGreeting:char_default"))
                .thenReturn("Good morning");
        when(notificationTokenRepository.findByUserIdAndIsActiveTrue("user_123"))
                .thenReturn(List.of());
        when(preferencesRepository.findByUserIdAndCharacterId("user_123", "char_default"))
                .thenReturn(Optional.of(preferences));

        worker.deliverDueNotifications();

        ArgumentCaptor<NotificationHistoryItem> historyCaptor =
                ArgumentCaptor.forClass(NotificationHistoryItem.class);
        verify(notificationHistoryService).addNotification(eq("user_123"), historyCaptor.capture());
        assertEquals(NotificationEvent.MORNING_GREETING, historyCaptor.getValue().type());
        assertEquals("Good morning", historyCaptor.getValue().body());

        ArgumentCaptor<NotificationEvent> requeueCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationDelayQueue).enqueue(requeueCaptor.capture());
        assertEquals(NotificationEvent.MORNING_GREETING, requeueCaptor.getValue().type());
        assertTrue(requeueCaptor.getValue().targetTimeEpoch() > event.targetTimeEpoch());
        verify(greetingService, never()).getDailyGreeting(anyString(), anyString());
    }
}
