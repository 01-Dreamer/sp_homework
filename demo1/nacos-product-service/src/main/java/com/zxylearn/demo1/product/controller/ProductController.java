package com.zxylearn.demo1.product.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import com.zxylearn.demo1.product.dto.ProductResponse;
import com.zxylearn.demo1.product.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Map<String, Product> PRODUCTS = Map.of(
            "2001", new Product("2001", "Keyboard", BigDecimal.valueOf(199.00)),
            "2002", new Product("2002", "Mouse", BigDecimal.valueOf(79.00)),
            "2003", new Product("2003", "Monitor", BigDecimal.valueOf(899.00))
    );

    private final int serverPort;

    public ProductController(@Value("${server.port}") int serverPort) {
        this.serverPort = serverPort;
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable("productId") String productId) {
        Product product = PRODUCTS.getOrDefault(productId, new Product(productId, "Default Product", BigDecimal.valueOf(49.00)));
        return new ProductResponse(
                product.id(),
                product.name(),
                product.price(),
                "nacos-product-service:" + serverPort,
                "nacos",
                Instant.now()
        );
    }
}
