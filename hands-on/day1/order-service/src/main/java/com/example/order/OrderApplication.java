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
     * Plain RestTemplate — no timeout, no retry, no circuit breaker.
     * This is intentional for Day 1 / Lab 1: we want threads to block
     * indefinitely on a slow dependency so we can observe thread exhaustion.
     * Day 2 adds resilience on top of this baseline.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
