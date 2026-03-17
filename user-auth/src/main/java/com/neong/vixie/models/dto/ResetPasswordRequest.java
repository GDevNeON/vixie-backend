package com.neong.vixie.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "OTP code is required")
        String code,

        @NotBlank(message = "New password is required")
        @JsonProperty("new_password")
        String newPassword
) {
}
