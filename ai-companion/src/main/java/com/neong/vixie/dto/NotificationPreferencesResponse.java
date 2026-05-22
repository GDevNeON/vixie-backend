package com.neong.vixie.dto;

import com.neong.vixie.model.NotificationPreferences;

import java.time.LocalTime;

public record NotificationPreferencesResponse(
        String characterId,
        boolean greetingEnabled,
        LocalTime greetingTime,
        boolean focusEnabled,
        LocalTime focusTime,
        boolean sleepEnabled,
        LocalTime sleepTime,
        String timezone
) {
    public static NotificationPreferencesResponse from(NotificationPreferences preferences) {
        preferences.applyDefaults();
        return new NotificationPreferencesResponse(
                preferences.getCharacterId(),
                preferences.isGreetingEnabled(),
                preferences.getGreetingTime(),
                preferences.isFocusEnabled(),
                preferences.getFocusTime(),
                preferences.isSleepEnabled(),
                preferences.getSleepTime(),
                preferences.getTimezone()
        );
    }
}
