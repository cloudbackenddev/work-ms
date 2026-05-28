package com.example.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client for inventory-service.
 *
 * Day 2 additions:
 *  - @CircuitBreaker: stops calling inventory-service when it's consistently failing
 *  - @Retry: retries transient failures (connection reset, pod restarting)
 *  - Fallback methods: return safe defaults when the circuit is OPEN
 *
 * The circuit breaker name "inventory-service" matches the key in application.yml
 * under resilience4j.circuitbreaker.instances.
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

    // ── getProduct ────────────────────────────────────────────────────────────

    /**
     * Get product details from inventory-service.
     *
     * @CircuitBreaker wraps this call. If inventory-service fails repeatedly,
     * the circuit opens and getProductFallback() is called immediately
     * without making a network request.
     *
     * @Retry retries up to 3 times on ResourceAccessException (connection refused,
     * timeout) before counting as a failure toward the circuit breaker threshold.
     */
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "getProductFallback")
    @Retry(name = "inventory-service")
    public ProductDto getProduct(Long productId) {
        String url = inventoryServiceUrl + "/inventory/" + productId;
        log.debug("GET {}", url);
        return restTemplate.getForObject(url, ProductDto.class);
    }

    /**
     * Fallback for getProduct — called when circuit is OPEN or all retries exhausted.
     *
     * Returns null to signal "product unavailable" to the caller.
     * OrderService checks for null and returns a meaningful error to the client.
     */
    private ProductDto getProductFallback(Long productId, Exception ex) {
        log.warn("inventory-service unavailable for productId={} — circuit open or retries exhausted. Cause: {}",
            productId, ex.getMessage());
        return null;
    }

    // ── reserveStock ──────────────────────────────────────────────────────────

    /**
     * Reserve stock in inventory-service.
     *
     * If the circuit is OPEN, reserveStockFallback() is called.
     * The order will be saved with status PENDING for later retry.
     */
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "reserveStockFallback")
    @Retry(name = "inventory-service")
    public boolean reserveStock(Long productId, int quantity) {
        String url = inventoryServiceUrl + "/inventory/" + productId + "/reserve";
        log.info("POST {} quantity={}", url, quantity);
        restTemplate.postForObject(url, Map.of("quantity", quantity), Object.class);
        return true;
    }

    /**
     * Fallback for reserveStock — circuit is OPEN.
     * Returns false so OrderService can save the order as PENDING.
     */
    private boolean reserveStockFallback(Long productId, int quantity, Exception ex) {
        log.warn("Cannot reserve stock for productId={} — inventory-service unavailable. Cause: {}",
            productId, ex.getMessage());
        return false;
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    /**
     * Minimal projection of inventory-service's Product.
     * Only the fields order-service actually needs — not the full entity.
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
