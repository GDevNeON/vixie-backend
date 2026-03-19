package com.neong.vixie.models.dto;

import com.neong.vixie.models.constant.Gender;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, digits, and underscores")
        String username,

        @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
        String displayName,

        @Size(max = 500, message = "Bio must not exceed 500 characters")
        String bio,

        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format")
        String phoneNumber,

        Gender gender,

        LocalDate dateOfBirth,

        @Size(min = 2, max = 2, message = "Country must be a 2-letter ISO 3166-1 alpha-2 code")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be uppercase ISO 3166-1 alpha-2")
        String country,

        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location
) {
}
