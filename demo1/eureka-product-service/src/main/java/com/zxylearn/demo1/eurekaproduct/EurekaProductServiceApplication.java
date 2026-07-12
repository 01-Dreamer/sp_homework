package com.zxylearn.demo1.eurekaproduct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class EurekaProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaProductServiceApplication.class, args);
    }
}
