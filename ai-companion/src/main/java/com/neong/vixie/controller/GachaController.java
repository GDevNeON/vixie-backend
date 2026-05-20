package com.neong.vixie.controller;

import com.neong.vixie.service.GachaService;
import com.neong.vixie.service.GachaService.PullResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
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
            java.security.Principal principal,
            @Valid @RequestBody PullRequest request) {
        String userId = principal.getName();
        PullResponse response = gachaService.pull(userId, request.bannerId(), request.count());
        return ResponseEntity.ok(response);
    }

    public record PullRequest(
            @NotBlank @JsonProperty("banner_id") String bannerId,
            int count // 1 or 10
    ) {}
}
