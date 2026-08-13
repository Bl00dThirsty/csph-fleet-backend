package com.gpl.cylinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CylinderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CylinderServiceApplication.class, args);
    }
}
