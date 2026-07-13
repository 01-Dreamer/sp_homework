package com.zxylearn.demo4.product.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import com.zxylearn.demo4.product.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final int serverPort;
    private final String instanceName;

    public ProductController(
            @Value("${server.port}") int serverPort,
            @Value("${demo4.instance-name}") String instanceName
    ) {
        this.serverPort = serverPort;
        this.instanceName = instanceName;
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(
            @PathVariable("productId") String productId,
            @RequestParam(name = "delayMillis", defaultValue = "0") long delayMillis,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail
    ) throws InterruptedException {
        if (delayMillis > 0) {
            Thread.sleep(delayMillis);
        }
        if (fail) {
            throw new IllegalStateException("simulated product service failure");
        }
        return new ProductResponse(
                productId,
                "Gateway Lab Product " + productId,
                new BigDecimal("199.00"),
                instanceName + ":" + serverPort,
                Instant.now()
        );
    }

    @GetMapping("/instance")
    public Map<String, Object> instance() {
        return Map.of(
                "service", "demo4-product-service",
                "instance", instanceName,
                "port", serverPort,
                "checkedAt", Instant.now()
        );
    }
}
