package com.neong.vixie.dto;

import java.time.Instant;

public record NotificationHistoryItem(
        String id,
        String type,
        String title,
        String body,
        String characterId,
        Instant sentAt,
        boolean isRead
) {
    public NotificationHistoryItem markRead() {
        return new NotificationHistoryItem(id, type, title, body, characterId, sentAt, true);
    }
}
