package com.neong.vixie.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MobileOAuthLoginRequest(
        @NotBlank String provider,
        @NotBlank String idToken,
        @NotBlank @Email String email,
        String username
) {
}
