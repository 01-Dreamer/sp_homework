package com.zxylearn.demo5.configcart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
@ConfigurationPropertiesScan
@SpringBootApplication
public class ConfigCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigCartApplication.class, args);
    }
}
