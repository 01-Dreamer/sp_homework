package com.zxylearn.demo3.product.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import com.zxylearn.demo3.product.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Map<String, ProductData> PRODUCTS = Map.of(
            "3001", new ProductData("3001", "Keyboard", new BigDecimal("199.00")),
            "3002", new ProductData("3002", "Mouse", new BigDecimal("79.00")),
            "3003", new ProductData("3003", "Monitor", new BigDecimal("899.00"))
    );

    private final int serverPort;

    public ProductController(@Value("${server.port}") int serverPort) {
        this.serverPort = serverPort;
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "0") long delayMillis
    ) throws InterruptedException {
        if (delayMillis > 0) {
            Thread.sleep(delayMillis);
        }
        if (fail) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Product service simulated failure");
        }
        ProductData product = PRODUCTS.getOrDefault(productId, new ProductData(productId, "Default Product", new BigDecimal("49.00")));
        return new ProductResponse(
                product.id(),
                product.name(),
                product.price(),
                "product-service:" + serverPort,
                delayMillis,
                Instant.now()
        );
    }

    private record ProductData(String id, String name, BigDecimal price) {
    }
}
