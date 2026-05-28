package com.example.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for inventory-service.
 *
 * Day 1 concepts demonstrated here:
 *  - Embedded Tomcat starts on the configured port (12-Factor VII)
 *  - No external app server required — the JAR IS the service
 *  - Graceful shutdown is configured in application.yml (12-Factor IX)
 */
@SpringBootApplication
public class InventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
