package com.neong.vixie.dto;

public record NotificationEvent(
        String userId,
        String characterId,
        String type,
        long targetTimeEpoch,
        String occasionId
) {
    public static final String MORNING_GREETING = "MORNING_GREETING";
    public static final String FOCUS = "FOCUS";
    public static final String SLEEP = "SLEEP";
    public static final String OCCASION = "OCCASION";
}
