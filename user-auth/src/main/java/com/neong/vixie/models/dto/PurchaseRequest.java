package com.neong.vixie.models.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PurchaseRequest(
        @NotBlank String itemId,
        @Min(1) int expectedPriceCoins
) {}
