package com.zxylearn.demo1.eureka.servera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class EurekaServerAApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerAApplication.class, args);
    }
}
