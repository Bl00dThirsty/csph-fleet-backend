package com.gpl.subsidy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SubsidyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubsidyServiceApplication.class, args);
    }
}
