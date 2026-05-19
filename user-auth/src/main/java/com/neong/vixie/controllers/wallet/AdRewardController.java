package com.neong.vixie.controllers.wallet;

import com.neong.vixie.models.dto.WalletBalanceResponse;
import com.neong.vixie.services.wallet.AdRewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdRewardController {

    private final AdRewardService adRewardService;

    /**
     * POST /api/coins/ad-reward — Grant 1 coin for watching an ad.
     * Returns 429 if daily cap exceeded.
     */
    @PostMapping("/api/coins/ad-reward")
    public ResponseEntity<WalletBalanceResponse> claimAdReward(
            @AuthenticationPrincipal UserDetails user) {
        int newBalance = adRewardService.rewardAdWatch(getUserId(user));
        return ResponseEntity.ok(new WalletBalanceResponse(newBalance));
    }

    /**
     * GET /api/coins/ad-remaining — How many ads can user still watch today.
     */
    @GetMapping("/api/coins/ad-remaining")
    public ResponseEntity<Map<String, Integer>> getAdRemaining(
            @AuthenticationPrincipal UserDetails user) {
        int remaining = adRewardService.getRemainingAdWatches(getUserId(user));
        return ResponseEntity.ok(Map.of("remaining", remaining));
    }

    private String getUserId(UserDetails user) {
        if (user instanceof com.neong.vixie.models.db.User u) {
            return u.getId();
        }
        throw new IllegalStateException("Unexpected UserDetails type");
    }
}
