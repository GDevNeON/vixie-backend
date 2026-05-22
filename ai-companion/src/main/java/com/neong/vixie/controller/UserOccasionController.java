package com.neong.vixie.controller;

import com.neong.vixie.dto.UserOccasionRequest;
import com.neong.vixie.dto.UserOccasionResponse;
import com.neong.vixie.model.UserOccasion;
import com.neong.vixie.repository.UserOccasionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications/occasions")
@RequiredArgsConstructor
public class UserOccasionController {

    private final UserOccasionRepository userOccasionRepository;

    @GetMapping
    public ResponseEntity<List<UserOccasionResponse>> getOccasions(Principal principal) {
        List<UserOccasionResponse> occasions = userOccasionRepository.findByUserId(principal.getName())
                .stream()
                .map(UserOccasionResponse::from)
                .toList();
        return ResponseEntity.ok(occasions);
    }

    @PostMapping
    public ResponseEntity<UserOccasionResponse> saveOccasion(
            @Valid @RequestBody UserOccasionRequest request,
            Principal principal) {
        String userId = principal.getName();
        UserOccasion occasion = request.id() == null
                ? new UserOccasion()
                : userOccasionRepository.findByIdAndUserId(request.id(), userId)
                        .orElseGet(UserOccasion::new);

        occasion.setUserId(userId);
        occasion.setType(request.type());
        occasion.setLabel(request.label());
        occasion.setOccasionDate(request.occasionDate());
        occasion.setNotificationEnabled(!Boolean.FALSE.equals(request.notificationEnabled()));
        occasion.setDetectedFromChat(Boolean.TRUE.equals(request.detectedFromChat()));
        occasion.setConfirmedByUser(Boolean.TRUE.equals(request.confirmedByUser())
                || Boolean.TRUE.equals(request.detectedFromChat()));

        UserOccasion saved = userOccasionRepository.save(occasion);
        return ResponseEntity.ok(UserOccasionResponse.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOccasion(@PathVariable String id, Principal principal) {
        userOccasionRepository.findByIdAndUserId(id, principal.getName())
                .ifPresent(userOccasionRepository::delete);
        return ResponseEntity.noContent().build();
    }
}
