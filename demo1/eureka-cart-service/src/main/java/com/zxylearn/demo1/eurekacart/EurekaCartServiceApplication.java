package com.zxylearn.demo1.eurekacart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class EurekaCartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaCartServiceApplication.class, args);
    }
}
