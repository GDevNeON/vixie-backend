package com.neong.vixie.service;

import com.neong.vixie.dto.NotificationEvent;
import com.neong.vixie.model.NotificationPreferences;
import com.neong.vixie.repository.NotificationPreferencesRepository;
import com.neong.vixie.repository.UserOccasionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GreetingPreGenerationJobTest {

    @Mock private NotificationPreferencesRepository preferencesRepository;
    @Mock private UserOccasionRepository userOccasionRepository;
    @Mock private GreetingService greetingService;
    @Mock private NotificationDelayQueue notificationDelayQueue;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private GreetingPreGenerationJob job;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        job = new GreetingPreGenerationJob(
                preferencesRepository, userOccasionRepository, greetingService,
                notificationDelayQueue, stringRedisTemplate);
    }

    @Test
    void run_preGeneratesGreetingAndEnqueuesMorningEventOneHourBeforeLocalTime() {
        LocalTime oneHourFromNow = LocalTime.now(ZoneOffset.UTC).plusHours(1).withSecond(0).withNano(0);
        NotificationPreferences preferences = NotificationPreferences.builder()
                .userId("user_123")
                .characterId("char_default")
                .greetingEnabled(true)
                .greetingTime(oneHourFromNow)
                .timezone("UTC")
                .build();
        when(preferencesRepository.findAll()).thenReturn(List.of(preferences));
        when(userOccasionRepository.findByUserIdAndOccasionDateAndNotificationEnabledTrue(eq("user_123"), anyString()))
                .thenReturn(List.of());
        when(greetingService.getDailyGreeting("user_123", "char_default"))
                .thenReturn(Map.of("message", "Good morning"));

        job.run();

        verify(valueOps).set(
                eq("vixie:user:user_123:pregeneratedGreeting:char_default"),
                eq("Good morning"),
                eq(Duration.ofHours(2))
        );
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationDelayQueue).enqueue(captor.capture());
        assertEquals(NotificationEvent.MORNING_GREETING, captor.getValue().type());
        assertEquals("user_123", captor.getValue().userId());
    }
}
