package com.zxylearn.demo4.cart.dto;

import java.time.Instant;
import java.util.List;

public record CartResponse(
        String cartId,
        List<CartItem> items,
        String servedBy,
        Instant checkedAt
) {
}
