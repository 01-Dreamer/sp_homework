package com.zxylearn.demo5.configcart.client;

import com.zxylearn.demo5.configcart.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "config-product-service", url = "${demo5.remote.product-url:http://localhost:8311}", path = "/api/products")
public interface ProductClient {

    @GetMapping("/{productId}")
    ProductResponse getProduct(@PathVariable("productId") String productId);
}
