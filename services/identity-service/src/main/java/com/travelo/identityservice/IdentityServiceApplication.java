package com.travelo.identityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.travelo.authservice",
        "com.travelo.userservice",
        "com.travelo.identityservice",
})
@EntityScan(basePackages = {
        "com.travelo.authservice.entity",
        "com.travelo.userservice.entity",
})
@EnableJpaRepositories(basePackages = {
        "com.travelo.authservice.repository",
        "com.travelo.userservice.repository",
})
@EnableDiscoveryClient
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
