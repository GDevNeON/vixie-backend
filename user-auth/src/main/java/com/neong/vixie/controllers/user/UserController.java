package com.neong.vixie.controllers.user;

import com.neong.vixie.models.db.User;
import com.neong.vixie.models.dto.UserMeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public UserMeResponse me(@AuthenticationPrincipal User user) {
        return UserMeResponse.fromUser(user);
    }
}
