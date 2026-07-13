package com.zxylearn.demo4.cart.dto;

public record CartItem(
        String productId,
        String productName,
        int quantity
) {
}
