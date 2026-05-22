package com.neong.vixie.dto;

import com.neong.vixie.model.OccasionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserOccasionRequest(
        String id,
        @NotNull OccasionType type,
        @NotBlank String label,
        @NotBlank @Pattern(regexp = "^(0[1-9]|1[0-2])-[0-3][0-9]$") String occasionDate,
        Boolean notificationEnabled,
        Boolean detectedFromChat,
        Boolean confirmedByUser
) {
}
