package com.zxylearn.demo2.cart.client;

import com.zxylearn.demo2.cart.dto.DeleteResponse;
import com.zxylearn.demo2.cart.dto.InstanceResponse;
import com.zxylearn.demo2.cart.dto.ProductRequest;
import com.zxylearn.demo2.cart.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ProductFeignClient.SERVICE_NAME, path = "/api/products")
public interface ProductFeignClient {

    String SERVICE_NAME = "product-service";

    @GetMapping("/{productId}")
    ProductResponse getProduct(@PathVariable("productId") String productId);

    @PostMapping
    ProductResponse createProduct(@RequestBody ProductRequest request);

    @PutMapping("/{productId}")
    ProductResponse updateProduct(
            @PathVariable("productId") String productId,
            @RequestBody ProductRequest request
    );

    @DeleteMapping("/{productId}")
    DeleteResponse deleteProduct(@PathVariable("productId") String productId);

    @GetMapping("/ping")
    InstanceResponse ping();
}
