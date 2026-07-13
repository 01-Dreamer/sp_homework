package com.zxylearn.demo3.sentinel.client;

import com.zxylearn.demo3.sentinel.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", path = "/api/products")
public interface ProductClient {

    @GetMapping("/{productId}")
    ProductResponse getProduct(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail") boolean fail,
            @RequestParam(name = "delayMillis") long delayMillis
    );
}
