package com.neong.vixie.controllers.user;

import com.neong.vixie.models.db.NotificationToken;
import com.neong.vixie.models.db.User;
import com.neong.vixie.models.dto.NotificationTokenRequest;
import com.neong.vixie.repositories.user.NotificationTokenRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
        String userId = resolveUserId(principal);

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

    private String resolveUserId(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return principal.getName();
    }
}
