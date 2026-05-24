package com.neong.vixie.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for registering/updating an FCM token.
 */
public record NotificationTokenRequest(
        @JsonAlias("deviceId")
        @NotBlank String deviceId,
        @JsonAlias("fcmToken")
        @NotBlank String fcmToken,
        @NotBlank String platform
) {}
