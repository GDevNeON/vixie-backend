package com.neong.vixie.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        String username,
        String firstName,
        String lastName,
        String countryOfOrigin
) {
}
