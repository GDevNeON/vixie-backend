package com.neong.vixie.controller;

import com.neong.vixie.dto.NotificationTokenRequest;
import com.neong.vixie.model.NotificationToken;
import com.neong.vixie.repository.NotificationTokenRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.Optional;

/**
 * Endpoint for registering/updating FCM device tokens.
 * POST /api/notifications/token — upsert by (userId, deviceId)
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationTokenController {

    private final NotificationTokenRepository notificationTokenRepository;

    @PostMapping("/token")
    public ResponseEntity<Void> registerToken(
            @Valid @RequestBody NotificationTokenRequest request,
            Principal principal) {
        String userId = principal.getName();

        Optional<NotificationToken> existing =
                notificationTokenRepository.findByUserIdAndDeviceId(userId, request.deviceId());

        if (existing.isPresent()) {
            NotificationToken token = existing.get();
            token.setFcmToken(request.fcmToken());
            token.setPlatform(request.platform());
            token.setActive(true);
            token.setUpdatedAt(Instant.now());
            notificationTokenRepository.save(token);
        } else {
            NotificationToken token = NotificationToken.builder()
                    .userId(userId)
                    .deviceId(request.deviceId())
                    .fcmToken(request.fcmToken())
                    .platform(request.platform())
                    .isActive(true)
                    .build();
            notificationTokenRepository.save(token);
        }

        return ResponseEntity.ok().build();
    }
}
