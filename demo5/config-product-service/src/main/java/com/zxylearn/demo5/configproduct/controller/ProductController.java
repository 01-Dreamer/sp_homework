package com.zxylearn.demo5.configproduct.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

import com.zxylearn.demo5.configproduct.config.ProductProperties;
import com.zxylearn.demo5.configproduct.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductProperties properties;
    private final String applicationName;
    private final String port;

    public ProductController(
            ProductProperties properties,
            @Value("${spring.application.name}") String applicationName,
            @Value("${server.port}") String port) {
        this.properties = properties;
        this.applicationName = applicationName;
        this.port = port;
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable String productId) {
        BigDecimal originalPrice = new BigDecimal("199.00");
        BigDecimal salePrice = originalPrice.multiply(properties.getDiscountRate()).setScale(2, RoundingMode.HALF_UP);
        return new ProductResponse(
                productId,
                "Demo5 Config 商品 " + productId,
                originalPrice,
                salePrice,
                properties.getCurrency(),
                properties.getMessage(),
                properties.getVersion(),
                applicationName + ":" + port,
                Instant.now());
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of(
                "message", properties.getMessage(),
                "version", properties.getVersion(),
                "discountRate", properties.getDiscountRate(),
                "currency", properties.getCurrency(),
                "servedBy", applicationName + ":" + port);
    }
}
