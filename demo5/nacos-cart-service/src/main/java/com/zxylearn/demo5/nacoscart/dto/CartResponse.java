package com.zxylearn.demo5.nacoscart.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CartResponse(
        String cartId,
        ProductResponse product,
        int quantity,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        String configMessage,
        String configVersion,
        String servedBy,
        Instant createdAt) {
}
