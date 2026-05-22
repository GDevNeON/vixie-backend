package com.neong.vixie.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalTime;

public record NotificationPreferencesRequest(
        @NotBlank String characterId,
        Boolean greetingEnabled,
        LocalTime greetingTime,
        Boolean focusEnabled,
        LocalTime focusTime,
        Boolean sleepEnabled,
        LocalTime sleepTime,
        String timezone
) {
}
