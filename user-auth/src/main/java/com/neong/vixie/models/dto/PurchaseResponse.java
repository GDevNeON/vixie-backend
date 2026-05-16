package com.neong.vixie.models.dto;

public record PurchaseResponse(
        boolean success,
        int newBalance,
        String itemId
) {}
