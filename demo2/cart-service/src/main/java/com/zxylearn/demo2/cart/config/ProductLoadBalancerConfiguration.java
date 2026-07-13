package com.zxylearn.demo2.cart.config;

import com.zxylearn.demo2.cart.client.ProductFeignClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("random")
@Configuration(proxyBeanMethods = false)
@LoadBalancerClient(name = ProductFeignClient.SERVICE_NAME, configuration = RandomLoadBalancerConfiguration.class)
public class ProductLoadBalancerConfiguration {
}
