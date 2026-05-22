package com.neong.vixie.dto;

import com.neong.vixie.model.OccasionType;
import com.neong.vixie.model.UserOccasion;

public record UserOccasionResponse(
        String id,
        OccasionType type,
        String label,
        String occasionDate,
        boolean notificationEnabled,
        boolean detectedFromChat,
        boolean confirmedByUser
) {
    public static UserOccasionResponse from(UserOccasion occasion) {
        return new UserOccasionResponse(
                occasion.getId(),
                occasion.getType(),
                occasion.getLabel(),
                occasion.getOccasionDate(),
                occasion.isNotificationEnabled(),
                occasion.isDetectedFromChat(),
                occasion.isConfirmedByUser()
        );
    }
}
