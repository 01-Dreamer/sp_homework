package com.zxylearn.demo4.cart.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.zxylearn.demo4.cart.dto.CartItem;
import com.zxylearn.demo4.cart.dto.CartResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final int serverPort;

    public CartController(@Value("${server.port}") int serverPort) {
        this.serverPort = serverPort;
    }

    @GetMapping("/{cartId}")
    public CartResponse getCart(@PathVariable("cartId") String cartId) {
        return new CartResponse(
                cartId,
                List.of(
                        new CartItem("4001", "Gateway Lab Product 4001", 2),
                        new CartItem("4002", "Gateway Lab Product 4002", 1)
                ),
                "cart-service:" + serverPort,
                Instant.now()
        );
    }

    @GetMapping("/instance")
    public Map<String, Object> instance() {
        return Map.of(
                "service", "demo4-cart-service",
                "port", serverPort,
                "checkedAt", Instant.now()
        );
    }
}
