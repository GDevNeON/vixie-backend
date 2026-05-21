package com.neong.vixie.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for registering/updating an FCM token.
 */
public record NotificationTokenRequest(
        @NotBlank String deviceId,
        @NotBlank String fcmToken,
        @NotBlank String platform
) {}
