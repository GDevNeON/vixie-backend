package com.neong.vixie.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateItemRequest(
        @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
        String name,
        
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,
        
        @Min(value = 0, message = "Price cannot be negative")
        Integer priceCoins
) {}
