package com.zxylearn.demo5.nacoscart.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import com.zxylearn.demo5.nacoscart.client.ProductClient;
import com.zxylearn.demo5.nacoscart.config.CartProperties;
import com.zxylearn.demo5.nacoscart.dto.CartResponse;
import com.zxylearn.demo5.nacoscart.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final ProductClient productClient;
    private final CartProperties properties;
    private final String applicationName;
    private final String port;

    public CartController(
            ProductClient productClient,
            CartProperties properties,
            @Value("${spring.application.name}") String applicationName,
            @Value("${server.port}") String port) {
        this.productClient = productClient;
        this.properties = properties;
        this.applicationName = applicationName;
        this.port = port;
    }

    @GetMapping("/{cartId}")
    public CartResponse getCart(
            @PathVariable String cartId,
            @RequestParam(defaultValue = "5002") String productId,
            @RequestParam(defaultValue = "2") int quantity) {
        ProductResponse product = productClient.getProduct(productId);
        BigDecimal total = product.salePrice()
                .multiply(BigDecimal.valueOf(quantity))
                .add(properties.getDeliveryFee());
        return new CartResponse(
                cartId,
                product,
                quantity,
                properties.getDeliveryFee(),
                total,
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
                "deliveryFee", properties.getDeliveryFee(),
                "servedBy", applicationName + ":" + port);
    }
}
