package com.zxylearn.demo1.cart.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CartResponse(
        String cartId,
        String productId,
        int quantity,
        BigDecimal totalAmount,
        ProductResponse product,
        String servedBy,
        String registry,
        Instant createdAt
) {
}
