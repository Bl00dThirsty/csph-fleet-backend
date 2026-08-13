package com.gpl.fleet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FleetDeviceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FleetDeviceServiceApplication.class, args);
    }
}
