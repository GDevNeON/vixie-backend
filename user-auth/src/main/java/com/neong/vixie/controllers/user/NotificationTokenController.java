package com.neong.vixie.controllers.user;

import com.neong.vixie.models.db.NotificationToken;
import com.neong.vixie.models.dto.NotificationTokenRequest;
import com.neong.vixie.repositories.user.NotificationTokenRepository;
import jakarta.validation.Valid;
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
public class NotificationTokenController {

    private final NotificationTokenRepository notificationTokenRepository;

    public NotificationTokenController(NotificationTokenRepository notificationTokenRepository) {
        this.notificationTokenRepository = notificationTokenRepository;
    }

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
            NotificationToken token = new NotificationToken();
            token.setUserId(userId);
            token.setDeviceId(request.deviceId());
            token.setFcmToken(request.fcmToken());
            token.setPlatform(request.platform());
            token.setActive(true);
            notificationTokenRepository.save(token);
        }

        return ResponseEntity.ok().build();
    }
}
