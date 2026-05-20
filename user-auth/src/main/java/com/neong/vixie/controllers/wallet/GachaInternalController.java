package com.neong.vixie.controllers.wallet;

import com.neong.vixie.services.wallet.GachaInternalService;
import com.neong.vixie.services.wallet.GachaInternalService.PullResultEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Internal endpoints for ai-companion service to call.
 * Secured by X-Service-Key header (shared secret).
 */
@RestController
@RequiredArgsConstructor
public class GachaInternalController {

    private final GachaInternalService gachaInternalService;
    private final com.neong.vixie.repositories.user.UserRepository userRepository;

    @Value("${vixie.internal.service-key:}")
    private String serviceKey;

    /**
     * GET /api/internal/gacha/pity?userId=...&bannerId=...
     */
    @GetMapping("/api/internal/gacha/pity")
    public ResponseEntity<Map<String, Integer>> getPity(
            @RequestHeader("X-Service-Key") String key,
            @RequestParam String userId,
            @RequestParam String bannerId) {
        validateServiceKey(key);
        String realUserId = resolveUserId(userId);
        int pity = gachaInternalService.getPityCount(realUserId, bannerId);
        return ResponseEntity.ok(Map.of("current_pity", pity));
    }

    /**
     * POST /api/internal/gacha/commit
     */
    @PostMapping("/api/internal/gacha/commit")
    public ResponseEntity<Map<String, Object>> commitPulls(
            @RequestHeader("X-Service-Key") String key,
            @Valid @RequestBody CommitPullsRequest request) {
        validateServiceKey(key);
        String realUserId = resolveUserId(request.userId());
        int newBalance = gachaInternalService.commitPulls(
                realUserId,
                request.bannerId(),
                request.totalCost(),
                request.pullResults().stream()
                        .map(r -> new PullResultEntry(r.itemId(), r.rarity()))
                        .toList()
        );
        return ResponseEntity.ok(Map.of(
                "success", true,
                "new_balance", newBalance
        ));
    }

    private void validateServiceKey(String key) {
        if (serviceKey.isBlank() || !serviceKey.equals(key)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid service key");
        }
    }

    private String resolveUserId(String userIdOrEmail) {
        if (userIdOrEmail.contains("@")) {
            return userRepository.findByEmail(userIdOrEmail)
                    .map(com.neong.vixie.models.db.User::getId)
                    .orElse(userIdOrEmail);
        }
        return userIdOrEmail;
    }

    public record CommitPullsRequest(
            @NotBlank String userId,
            @NotBlank String bannerId,
            @Positive int totalCost,
            @NotEmpty List<PullResultItemRequest> pullResults
    ) {}

    public record PullResultItemRequest(
            @NotBlank String itemId,
            @NotBlank String rarity
    ) {}
}
