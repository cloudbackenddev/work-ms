package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    /**
     * RestTemplate bean — used by InventoryClient to call inventory-service.
     * Declared here so Spring manages its lifecycle and it can be injected.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
