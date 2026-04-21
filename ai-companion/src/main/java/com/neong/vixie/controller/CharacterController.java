package com.neong.vixie.controller;

import com.neong.vixie.dto.CharacterResponse;
import com.neong.vixie.dto.CharacterStateResponse;
import com.neong.vixie.dto.PersonalityPatchRequest;
import com.neong.vixie.service.CharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * REST controller for character endpoints.
 * - GET /api/characters — list all characters
 * - GET /api/characters/{id} — get character details
 * - GET /api/characters/{id}/state — get user-specific character state
 * - PATCH /api/characters/{id}/personality — update personality sliders
 */
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping
    public ResponseEntity<List<CharacterResponse>> getAllCharacters() {
        return ResponseEntity.ok(characterService.getAllCharacters());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CharacterResponse> getCharacter(@PathVariable String id) {
        return ResponseEntity.ok(characterService.getCharacter(id));
    }

    @GetMapping("/{id}/state")
    public ResponseEntity<CharacterStateResponse> getCharacterState(
            @PathVariable String id, Principal principal) {
        String userId = principal.getName();
        return ResponseEntity.ok(characterService.getCharacterState(userId, id));
    }

    @PatchMapping("/{id}/personality")
    public ResponseEntity<Void> updatePersonality(
            @PathVariable String id,
            @RequestBody PersonalityPatchRequest request,
            Principal principal) {
        String userId = principal.getName();
        characterService.updatePersonality(userId, id, request);
        return ResponseEntity.noContent().build();
    }
}
