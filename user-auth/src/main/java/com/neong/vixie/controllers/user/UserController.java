package com.neong.vixie.controllers.user;

import com.neong.vixie.models.db.User;
import com.neong.vixie.models.dto.UserMeResponse;
import com.neong.vixie.models.dto.UserProfileResponse;
import com.neong.vixie.services.user.ProfileService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ProfileService profileService;

    public UserController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public UserMeResponse me(@AuthenticationPrincipal User user) {
        UserProfileResponse profile = profileService.getProfile(user.getId());
        return UserMeResponse.fromUser(user, profile);
    }
}
