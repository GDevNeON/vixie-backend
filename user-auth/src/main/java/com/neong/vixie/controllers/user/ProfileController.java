package com.neong.vixie.controllers.user;

import com.neong.vixie.models.db.User;
import com.neong.vixie.models.dto.UpdateAvatarRequest;
import com.neong.vixie.models.dto.UpdateProfileRequest;
import com.neong.vixie.models.dto.UserProfileResponse;
import com.neong.vixie.services.user.ProfileService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public UserProfileResponse getProfile(@AuthenticationPrincipal User user) {
        return profileService.getProfile(user.getId());
    }

    // @RateLimit(requests = 10, per = Duration.ofMinutes(1)) — enforcement deferred to Phase 2
    @PutMapping("/profile")
    public UserProfileResponse updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(user.getId(), request);
    }

    // @RateLimit(requests = 5, per = Duration.ofMinutes(1)) — enforcement deferred to Phase 2
    @PutMapping("/avatar")
    public UserProfileResponse updateAvatar(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateAvatarRequest request) {
        return profileService.updateAvatar(user.getId(), request.avatarUrl());
    }

    // @RateLimit(requests = 5, per = Duration.ofMinutes(1)) — enforcement deferred to Phase 2
    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileResponse uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        return profileService.uploadAvatar(user.getId(), file);
    }

    // @RateLimit(requests = 5, per = Duration.ofMinutes(1)) — enforcement deferred to Phase 2
    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAvatar(@AuthenticationPrincipal User user) {
        profileService.removeAvatar(user.getId());
    }
}
