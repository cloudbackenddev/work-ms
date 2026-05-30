package com.example.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client for inventory-service.
 *
 * Day 1 version — intentionally bare:
 *   - No timeout: threads block until inventory-service responds (or never)
 *   - No retry: a failure is a failure
 *   - No circuit breaker: every request goes through regardless of failure rate
 *
 * This is the baseline that makes Lab 1 (cascading failure) observable.
 * Day 2 wraps this with @CircuitBreaker and @Retry.
 *
 * Lab 1 Step 3: change getProduct() to call /inventory/slow/{id}
 * to simulate a slow dependency.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InventoryClient(
            RestTemplate restTemplate,
            @Value("${inventory.service.url}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    /**
     * Get product details from inventory-service.
     *
     * Lab 1 Step 3: temporarily change this URL to:
     *   inventoryServiceUrl + "/inventory/slow/" + productId
     * to observe thread exhaustion. Revert after the lab.
     */
    public ProductDto getProduct(Long productId) {
        String url = inventoryServiceUrl + "/inventory/slow/" + productId;
        log.warn("Calling slow inventory endpoint — thread will block for ~8s: GET {}", url);
        return restTemplate.getForObject(url, ProductDto.class);
    }

    /**
     * Reserve stock in inventory-service.
     */
    public void reserveStock(Long productId, int quantity) {
        String url = inventoryServiceUrl + "/inventory/" + productId + "/reserve";
        log.info("POST {} quantity={}", url, quantity);
        restTemplate.postForObject(url, Map.of("quantity", quantity), Object.class);
    }

    /**
     * Minimal projection of inventory-service's Product.
     * Only the fields order-service actually needs.
     */
    public record ProductDto(
            Long id,
            String name,
            String sku,
            int stockQuantity,
            int reservedQuantity) {

        public int getAvailableQuantity() {
            return stockQuantity - reservedQuantity;
        }
    }
}
