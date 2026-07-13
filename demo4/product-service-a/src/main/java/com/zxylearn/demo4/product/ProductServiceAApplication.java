package com.zxylearn.demo4.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ProductServiceAApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceAApplication.class, args);
    }
}
