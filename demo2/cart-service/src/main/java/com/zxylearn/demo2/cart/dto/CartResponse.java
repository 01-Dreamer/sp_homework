package com.zxylearn.demo2.cart.dto;

import java.time.Instant;

public record CartResponse(
        String cartId,
        String productId,
        int quantity,
        ProductResponse product,
        String servedBy,
        String client,
        Instant createdAt
) {
}
