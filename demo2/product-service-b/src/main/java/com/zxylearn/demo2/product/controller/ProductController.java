package com.zxylearn.demo2.product.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.zxylearn.demo2.product.dto.DeleteResponse;
import com.zxylearn.demo2.product.dto.InstanceResponse;
import com.zxylearn.demo2.product.dto.ProductRequest;
import com.zxylearn.demo2.product.dto.ProductResponse;
import com.zxylearn.demo2.product.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Map<String, Product> PRODUCTS = new ConcurrentHashMap<>(Map.of(
            "2001", new Product("2001", "Keyboard", new BigDecimal("199.00")),
            "2002", new Product("2002", "Mouse", new BigDecimal("79.00")),
            "2003", new Product("2003", "Monitor", new BigDecimal("899.00"))
    ));

    private final int serverPort;
    private final String instanceId;
    private final String registry;

    public ProductController(
            @Value("${server.port}") int serverPort,
            @Value("${demo2.instance-id}") String instanceId,
            @Value("${demo2.registry}") String registry
    ) {
        this.serverPort = serverPort;
        this.instanceId = instanceId;
        this.registry = registry;
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable("productId") String productId) {
        Product product = findProduct(productId);
        return toResponse(product, "GET");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@RequestBody ProductRequest request) {
        String productId = request.productId() == null || request.productId().isBlank()
                ? UUID.randomUUID().toString()
                : request.productId();
        Product product = new Product(productId, request.name(), request.price());
        PRODUCTS.put(productId, product);
        return toResponse(product, "POST");
    }

    @PutMapping("/{productId}")
    public ProductResponse updateProduct(
            @PathVariable("productId") String productId,
            @RequestBody ProductRequest request
    ) {
        if (!PRODUCTS.containsKey(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + productId + " not found");
        }
        Product product = new Product(productId, request.name(), request.price());
        PRODUCTS.put(productId, product);
        return toResponse(product, "PUT");
    }

    @DeleteMapping("/{productId}")
    public DeleteResponse deleteProduct(@PathVariable("productId") String productId) {
        Product removed = PRODUCTS.remove(productId);
        if (removed == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + productId + " not found");
        }
        return new DeleteResponse(
                productId,
                true,
                servedBy(),
                registry,
                "DELETE",
                Instant.now()
        );
    }

    @GetMapping("/ping")
    public InstanceResponse ping() {
        return new InstanceResponse(
                "product-service",
                instanceId,
                serverPort,
                servedBy(),
                registry,
                Instant.now()
        );
    }

    private Product findProduct(String productId) {
        Product product = PRODUCTS.get(productId);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + productId + " not found");
        }
        return product;
    }

    private ProductResponse toResponse(Product product, String method) {
        return new ProductResponse(
                product.id(),
                product.name(),
                product.price(),
                servedBy(),
                registry,
                method,
                Instant.now()
        );
    }

    private String servedBy() {
        return "product-service:" + serverPort + "(" + instanceId + ")";
    }
}
