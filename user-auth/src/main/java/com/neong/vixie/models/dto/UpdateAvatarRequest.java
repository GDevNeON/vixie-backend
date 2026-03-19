package com.neong.vixie.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateAvatarRequest(
        @NotBlank(message = "Avatar URL must not be blank")
        @Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
        @URL(message = "Avatar URL must be a valid URL")
        String avatarUrl
) {
}
