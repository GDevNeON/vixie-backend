package com.neong.vixie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creator registration.
 */
public record CreatorRegistrationRequest(
        @NotBlank(message = "Display name is required")
        @Size(max = 100, message = "Display name must be 100 characters or less")
        String displayName,

        @Size(max = 500, message = "Bio must be 500 characters or less")
        String bio,

        boolean termsAccepted
) {}
