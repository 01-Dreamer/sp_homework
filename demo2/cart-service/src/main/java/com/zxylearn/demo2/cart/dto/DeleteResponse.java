package com.zxylearn.demo2.cart.dto;

import java.time.Instant;

public record DeleteResponse(
        String productId,
        boolean deleted,
        String servedBy,
        String registry,
        String method,
        Instant deletedAt
) {
}
