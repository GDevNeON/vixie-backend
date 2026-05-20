package com.neong.vixie.dto;

import java.time.Instant;

/**
 * Request DTO for internal purchase events from user-auth service.
 */
public record PurchaseEventRequest(
        String itemId,
        String buyerUserId,
        int priceCoins,
        Instant purchasedAt
) {}
