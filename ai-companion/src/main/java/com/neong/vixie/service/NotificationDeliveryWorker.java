package com.neong.vixie.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.neong.vixie.dto.NotificationEvent;
import com.neong.vixie.dto.NotificationHistoryItem;
import com.neong.vixie.model.NotificationPreferences;
import com.neong.vixie.model.NotificationToken;
import com.neong.vixie.model.OccasionType;
import com.neong.vixie.model.UserOccasion;
import com.neong.vixie.repository.NotificationPreferencesRepository;
import com.neong.vixie.repository.NotificationTokenRepository;
import com.neong.vixie.repository.UserOccasionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryWorker {

    private final NotificationDelayQueue notificationDelayQueue;
    private final NotificationHistoryService notificationHistoryService;
    private final NotificationTokenRepository notificationTokenRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final UserOccasionRepository userOccasionRepository;
    private final GreetingService greetingService;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(fixedDelay = 60000)
    public void deliverDueNotifications() {
        notificationDelayQueue.pollDueEvents().forEach(this::deliver);
    }

    private void deliver(NotificationEvent event) {
        NotificationContent content = resolveContent(event);
        List<NotificationToken> tokens =
                notificationTokenRepository.findByUserIdAndIsActiveTrue(event.userId());

        for (NotificationToken token : tokens) {
            sendFcm(token.getFcmToken(), event, content);
        }

        notificationHistoryService.addNotification(event.userId(), new NotificationHistoryItem(
                "notif_" + UUID.randomUUID(),
                event.type(),
                content.title(),
                content.body(),
                event.characterId(),
                Instant.now(),
                false
        ));
        requeueDaily(event);
    }

    private NotificationContent resolveContent(NotificationEvent event) {
        return switch (event.type()) {
            case NotificationEvent.MORNING_GREETING -> new NotificationContent(
                    "Morning greeting",
                    resolveMorningGreeting(event)
            );
            case NotificationEvent.FOCUS -> new NotificationContent(
                    "Focus time",
                    "Let's focus together!"
            );
            case NotificationEvent.SLEEP -> new NotificationContent(
                    "Sleep time",
                    "Time to rest! Goodnight."
            );
            case NotificationEvent.OCCASION -> resolveOccasionContent(event);
            default -> new NotificationContent(
                    "Vixie notification",
                    "Your companion has something for you."
            );
        };
    }

    private String resolveMorningGreeting(NotificationEvent event) {
        String cached = stringRedisTemplate.opsForValue().get(
                GreetingPreGenerationJob.greetingKey(event.userId(), event.characterId()));
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        Map<String, Object> result = greetingService.getDailyGreeting(
                event.userId(), event.characterId());
        Object message = result.get("message");
        if (message instanceof String greeting && !greeting.isBlank()) {
            return greeting;
        }
        return "Good morning! I'm glad to see you today.";
    }

    private NotificationContent resolveOccasionContent(NotificationEvent event) {
        if (event.occasionId() == null) {
            return new NotificationContent("Special day", "Your companion remembers today.");
        }
        return userOccasionRepository.findByIdAndUserId(event.occasionId(), event.userId())
                .map(this::occasionContent)
                .orElseGet(() -> new NotificationContent("Special day", "Your companion remembers today."));
    }

    private NotificationContent occasionContent(UserOccasion occasion) {
        if (occasion.getType() == OccasionType.BIRTHDAY) {
            return new NotificationContent("Happy birthday", "Happy birthday! Your companion is thinking of you today.");
        }
        if (occasion.getType() == OccasionType.ANNIVERSARY) {
            return new NotificationContent("Happy anniversary", "Happy anniversary! Your companion remembers this special day.");
        }
        return new NotificationContent(occasion.getLabel(), "Today is %s. Your companion is thinking of you.".formatted(occasion.getLabel()));
    }

    private void sendFcm(String fcmToken, NotificationEvent event, NotificationContent content) {
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(content.title())
                            .setBody(content.body())
                            .build())
                    .putData("type", event.type())
                    .putData("characterId", event.characterId())
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("FCM send failed for user={} character={}: {}",
                    event.userId(), event.characterId(), e.getMessage());
        }
    }

    private void requeueDaily(NotificationEvent event) {
        if (!isDailyRoutine(event.type())) {
            return;
        }

        preferencesRepository.findByUserIdAndCharacterId(event.userId(), event.characterId())
                .map(preferences -> nextDailyEvent(preferences, event.type()))
                .ifPresent(notificationDelayQueue::enqueue);
    }

    private boolean isDailyRoutine(String type) {
        return NotificationEvent.MORNING_GREETING.equals(type)
                || NotificationEvent.FOCUS.equals(type)
                || NotificationEvent.SLEEP.equals(type);
    }

    private NotificationEvent nextDailyEvent(NotificationPreferences preferences, String type) {
        preferences.applyDefaults();
        ZoneId zoneId = ZoneId.of(preferences.getTimezone());
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        java.time.LocalTime routineTime = timeFor(preferences, type);
        if (routineTime == null) {
            return null;
        }
        ZonedDateTime next = now.toLocalDate()
                .plusDays(1)
                .atTime(routineTime)
                .atZone(zoneId);

        return new NotificationEvent(
                preferences.getUserId(),
                preferences.getCharacterId(),
                type,
                next.toEpochSecond(),
                null
        );
    }

    private java.time.LocalTime timeFor(NotificationPreferences preferences, String type) {
        return switch (type) {
            case NotificationEvent.FOCUS -> preferences.getFocusTime();
            case NotificationEvent.SLEEP -> preferences.getSleepTime();
            default -> preferences.getGreetingTime();
        };
    }

    private record NotificationContent(String title, String body) {}
}
