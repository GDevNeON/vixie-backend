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
        preferences.applyDefaults();
        if (preferences.isGreetingEnabled()
                && isOneHourFromNow(preferences.getGreetingTime(), preferences.getTimezone())) {
            enqueueMatchingOccasions(preferences);
            preGenerateGreeting(preferences);
            enqueue(preferences, NotificationEvent.MORNING_GREETING);
        }
        if (preferences.isFocusEnabled()
                && isOneHourFromNow(preferences.getFocusTime(), preferences.getTimezone())) {
            enqueue(preferences, NotificationEvent.FOCUS);
        }
        if (preferences.isSleepEnabled()
                && isOneHourFromNow(preferences.getSleepTime(), preferences.getTimezone())) {
            enqueue(preferences, NotificationEvent.SLEEP);
        }
    }

    private void enqueueMatchingOccasions(NotificationPreferences preferences) {
        String today = ZonedDateTime.now(ZoneId.of(preferences.getTimezone())).format(OCCASION_DATE);
        userOccasionRepository
                .findByUserIdAndOccasionDateAndNotificationEnabledTrue(preferences.getUserId(), today)
                .forEach(occasion -> enqueueOccasion(preferences, occasion));
    }

    private void enqueueOccasion(NotificationPreferences preferences, UserOccasion occasion) {
        long targetEpoch = ZonedDateTime.now(ZoneId.of(preferences.getTimezone()))
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

    private boolean isOneHourFromNow(LocalTime routineTime, String timezone) {
        if (routineTime == null) {
            return false;
        }
        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime targetWindow = ZonedDateTime.now(zoneId).plusHours(1);
        return routineTime.getHour() == targetWindow.getHour()
                && routineTime.getMinute() == targetWindow.getMinute();
    }

    private void preGenerateGreeting(NotificationPreferences preferences) {
        try {
            Map<String, Object> result = greetingService.getDailyGreeting(
                    preferences.getUserId(), preferences.getCharacterId());
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

    private void enqueue(NotificationPreferences preferences, String type) {
        long targetEpoch = ZonedDateTime.now(ZoneId.of(preferences.getTimezone()))
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
