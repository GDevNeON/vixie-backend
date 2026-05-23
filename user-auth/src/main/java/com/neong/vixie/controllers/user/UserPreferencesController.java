package com.neong.vixie.controllers.user;

import com.neong.vixie.models.db.User;
import com.neong.vixie.models.db.UserPreferences;
import com.neong.vixie.models.dto.VoicePreferencesDto;
import com.neong.vixie.repositories.user.UserPreferencesRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/preferences")
public class UserPreferencesController {

    private final UserPreferencesRepository userPreferencesRepository;

    public UserPreferencesController(UserPreferencesRepository userPreferencesRepository) {
        this.userPreferencesRepository = userPreferencesRepository;
    }

    @GetMapping("/voice")
    public ResponseEntity<VoicePreferencesDto> getVoicePreferences(
            @AuthenticationPrincipal User user) {
        UserPreferences prefs = userPreferencesRepository.findByUserId(user.getId())
                .orElse(null);

        if (prefs == null) {
            // Return defaults when no preferences row exists
            return ResponseEntity.ok(new VoicePreferencesDto(false, 1.0));
        }

        return ResponseEntity.ok(new VoicePreferencesDto(
                prefs.getVoiceMuted() != null ? prefs.getVoiceMuted() : false,
                prefs.getVoiceVolume() != null ? prefs.getVoiceVolume() : 1.0
        ));
    }

    @PatchMapping("/voice")
    public ResponseEntity<VoicePreferencesDto> updateVoicePreferences(
            @AuthenticationPrincipal User user,
            @RequestBody VoicePreferencesDto request) {
        UserPreferences prefs = userPreferencesRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserPreferences newPrefs = new UserPreferences();
                    newPrefs.setId(com.neong.vixie.helpers.api.IdGenerator.generateId("preference"));
                    newPrefs.setUser(user);
                    return newPrefs;
                });

        if (request.voiceMuted() != null) {
            prefs.setVoiceMuted(request.voiceMuted());
        }
        if (request.voiceVolume() != null) {
            prefs.setVoiceVolume(request.voiceVolume());
        }

        userPreferencesRepository.save(prefs);

        return ResponseEntity.ok(new VoicePreferencesDto(
                prefs.getVoiceMuted(),
                prefs.getVoiceVolume()
        ));
    }
}
