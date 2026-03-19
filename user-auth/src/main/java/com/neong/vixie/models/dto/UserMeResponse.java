package com.neong.vixie.models.dto;

import com.neong.vixie.models.constant.AuthProvider;
import com.neong.vixie.models.constant.Role;
import com.neong.vixie.models.db.User;

public record UserMeResponse(
        String id,
        String email,
        Role role,
        AuthProvider authProvider
) {
    public static UserMeResponse fromUser(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getAuthProvider()
        );
    }
}
