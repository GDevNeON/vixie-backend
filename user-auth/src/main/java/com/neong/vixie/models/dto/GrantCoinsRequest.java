package com.neong.vixie.models.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record GrantCoinsRequest(
        @NotBlank String userId,
        @Min(1) int amount,
        @NotBlank String reason
) {}
