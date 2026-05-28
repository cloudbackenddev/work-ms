package com.example.inventory.health;

import com.example.inventory.repository.ProductRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom Actuator health indicator — Lab 4 demo.
 *
 * Appears at: GET /actuator/health
 * under the key "stockLevel"
 *
 * Reports WARN (still UP) when fewer than 10 products have available stock.
 * Reports DOWN when zero products have available stock.
 *
 * Kubernetes uses /actuator/health/readiness to decide whether to send traffic.
 * A DOWN status here will cause Kubernetes to stop routing requests to this pod.
 */
@Component
public class StockLevelHealthIndicator implements HealthIndicator {

    private final ProductRepository productRepository;

    // Threshold: warn when fewer than this many products are in stock
    private static final int WARN_THRESHOLD = 10;

    public StockLevelHealthIndicator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Health health() {
        long inStockCount = productRepository.findAllInStock().size();

        if (inStockCount == 0) {
            return Health.down()
                .withDetail("inStockProducts", 0)
                .withDetail("message", "No products available — service degraded")
                .build();
        }

        if (inStockCount < WARN_THRESHOLD) {
            return Health.up()
                .withDetail("inStockProducts", inStockCount)
                .withDetail("message", "Low stock warning — fewer than " + WARN_THRESHOLD + " products available")
                .build();
        }

        return Health.up()
            .withDetail("inStockProducts", inStockCount)
            .build();
    }
}
