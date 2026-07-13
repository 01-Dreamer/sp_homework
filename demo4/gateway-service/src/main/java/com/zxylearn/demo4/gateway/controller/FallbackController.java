package com.zxylearn.demo4.gateway.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/products")
    public Map<String, Object> productFallback() {
        return Map.of(
                "fallback", true,
                "route", "product-route",
                "message", "Gateway product route fallback",
                "createdAt", Instant.now()
        );
    }

    @GetMapping("/carts")
    public Map<String, Object> cartFallback() {
        return Map.of(
                "fallback", true,
                "route", "cart-route",
                "message", "Gateway cart route fallback",
                "createdAt", Instant.now()
        );
    }
}
