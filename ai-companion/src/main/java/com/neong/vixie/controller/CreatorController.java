package com.neong.vixie.controller;

import com.neong.vixie.dto.CreatorProfileDto;
import com.neong.vixie.dto.CreatorRegistrationRequest;
import com.neong.vixie.model.CreatorProfile;
import com.neong.vixie.service.CreatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * REST controller for creator operations.
 * All endpoints require authentication (secured in SecurityConfig).
 */
@RestController
@RequestMapping("/api/creator")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorService creatorService;

    /**
     * Register the current user as a creator.
     */
    @PostMapping("/register")
    public ResponseEntity<CreatorProfileDto> register(
            @Valid @RequestBody CreatorRegistrationRequest request,
            Principal principal) {
        String userId = principal.getName();

        if (!request.termsAccepted()) {
            throw new IllegalArgumentException("You must accept the creator terms to register");
        }

        CreatorProfile profile = creatorService.registerCreator(
                userId,
                request.displayName(),
                request.bio()
        );
        return ResponseEntity.ok(CreatorProfileDto.from(profile));
    }

    /**
     * Get the current user's creator profile.
     */
    @GetMapping("/profile")
    public ResponseEntity<CreatorProfileDto> getProfile(Principal principal) {
        String userId = principal.getName();
        return creatorService.getByUserId(userId)
                .map(CreatorProfileDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
