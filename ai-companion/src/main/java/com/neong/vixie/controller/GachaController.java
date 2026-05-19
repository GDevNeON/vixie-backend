package com.neong.vixie.controller;

import com.neong.vixie.service.GachaService;
import com.neong.vixie.service.GachaService.PullResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gacha pull endpoint — JWT required.
 */
@RestController
@RequiredArgsConstructor
public class GachaController {

    private final GachaService gachaService;

    @PostMapping("/api/gacha/pull")
    public ResponseEntity<PullResponse> pull(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody PullRequest request) {
        String userId = getUserId(user);
        PullResponse response = gachaService.pull(userId, request.bannerId(), request.count());
        return ResponseEntity.ok(response);
    }

    private String getUserId(UserDetails user) {
        // ai-companion JWT auth sets username as userId
        return user.getUsername();
    }

    public record PullRequest(
            @NotBlank String bannerId,
            int count // 1 or 10
    ) {}
}
