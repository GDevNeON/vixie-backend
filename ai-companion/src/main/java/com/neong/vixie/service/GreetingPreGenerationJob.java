package com.neong.vixie.service;

import com.neong.vixie.dto.NotificationEvent;
import com.neong.vixie.model.NotificationPreferences;
import com.neong.vixie.model.UserOccasion;
import com.neong.vixie.repository.NotificationPreferencesRepository;
import com.neong.vixie.repository.UserOccasionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GreetingPreGenerationJob {

    private static final Duration PREGENERATED_TTL = Duration.ofHours(2);
    private static final DateTimeFormatter OCCASION_DATE = DateTimeFormatter.ofPattern("MM-dd");

    private final NotificationPreferencesRepository preferencesRepository;
    private final UserOccasionRepository userOccasionRepository;
    private final GreetingService greetingService;
    private final NotificationDelayQueue notificationDelayQueue;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 * * * * *")
    public void run() {
        preferencesRepository.findAll().forEach(this::scheduleDueRoutines);
    }

    private void scheduleDueRoutines(NotificationPreferences preferences) {
        try {
            preferences.applyDefaults();
            ZoneId zoneId = ZoneId.of(preferences.getTimezone());
            if (preferences.isGreetingEnabled()
                    && isOneHourFromNow(preferences.getGreetingTime(), zoneId)) {
                enqueueMatchingOccasions(preferences, zoneId);
                preGenerateGreeting(preferences);
                enqueue(preferences, NotificationEvent.MORNING_GREETING, zoneId);
            }
            if (preferences.isFocusEnabled()
                    && isOneHourFromNow(preferences.getFocusTime(), zoneId)) {
                enqueue(preferences, NotificationEvent.FOCUS, zoneId);
            }
            if (preferences.isSleepEnabled()
                    && isOneHourFromNow(preferences.getSleepTime(), zoneId)) {
                enqueue(preferences, NotificationEvent.SLEEP, zoneId);
            }
        } catch (DateTimeException e) {
            log.warn("Skipping notification routines for user={} character={} due to invalid timezone '{}'",
                    preferences.getUserId(), preferences.getCharacterId(), preferences.getTimezone());
        }
    }

    private void enqueueMatchingOccasions(NotificationPreferences preferences, ZoneId zoneId) {
        String today = ZonedDateTime.now(zoneId).format(OCCASION_DATE);
        userOccasionRepository
                .findByUserIdAndOccasionDateAndNotificationEnabledTrue(preferences.getUserId(), today)
                .forEach(occasion -> enqueueOccasion(preferences, occasion, zoneId));
    }

    private void enqueueOccasion(NotificationPreferences preferences, UserOccasion occasion, ZoneId zoneId) {
        long targetEpoch = ZonedDateTime.now(zoneId)
                .plusHours(1)
                .toEpochSecond();
        notificationDelayQueue.enqueue(new NotificationEvent(
                preferences.getUserId(),
                preferences.getCharacterId(),
                NotificationEvent.OCCASION,
                targetEpoch,
                occasion.getId()
        ));
    }

    private boolean isOneHourFromNow(LocalTime routineTime, ZoneId zoneId) {
        if (routineTime == null) {
            return false;
        }
        ZonedDateTime targetWindow = ZonedDateTime.now(zoneId).plusHours(1);
        return routineTime.getHour() == targetWindow.getHour()
                && routineTime.getMinute() == targetWindow.getMinute();
    }

    private void preGenerateGreeting(NotificationPreferences preferences) {
        try {
            Map<String, Object> result = greetingService.getDailyGreeting(
                    preferences.getUserId(), preferences.getCharacterId(), null);
            Object message = result.get("message");
            if (message instanceof String greeting && !greeting.isBlank()) {
                stringRedisTemplate.opsForValue().set(
                        greetingKey(preferences.getUserId(), preferences.getCharacterId()),
                        greeting,
                        PREGENERATED_TTL
                );
            }
        } catch (Exception e) {
            log.warn("Greeting pre-generation failed for user={} character={}: {}",
                    preferences.getUserId(), preferences.getCharacterId(), e.getMessage());
        }
    }

    private void enqueue(NotificationPreferences preferences, String type, ZoneId zoneId) {
        long targetEpoch = ZonedDateTime.now(zoneId)
                .plusHours(1)
                .toEpochSecond();
        notificationDelayQueue.enqueue(new NotificationEvent(
                preferences.getUserId(),
                preferences.getCharacterId(),
                type,
                targetEpoch,
                null
        ));
    }

    static String greetingKey(String userId, String characterId) {
        return "vixie:user:%s:pregeneratedGreeting:%s".formatted(userId, characterId);
    }
}
