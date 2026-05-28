package com.example.order.service;

import com.example.order.client.InventoryClient;
import com.example.order.client.InventoryClient.ProductDto;
import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for order placement.
 *
 * Day 2 concepts demonstrated:
 *  - Circuit breaker fallback: order saved as PENDING when inventory-service is down
 *  - Idempotency: duplicate requests return the existing order (Lab 5)
 *  - Structured logging: all log statements include context for tracing
 */
@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository orderRepository, InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
    }

    // ── Place order ───────────────────────────────────────────────────────────

    /**
     * Place an order.
     *
     * Lab 5 — Idempotency:
     * If idempotencyKey is provided and we've seen it before,
     * return the existing order without creating a duplicate.
     *
     * Lab 2 — Circuit breaker:
     * If inventory-service is unavailable, InventoryClient returns null/false
     * and we save the order as PENDING rather than failing the request.
     */
    public Order placeOrder(Long productId, int quantity, String idempotencyKey) {

        // ── Idempotency check (Lab 5) ─────────────────────────────────────────
        if (idempotencyKey != null) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Duplicate request — returning existing order id={} idempotencyKey={}",
                    existing.get().getId(), idempotencyKey);
                return existing.get();
            }
        }

        // ── Check stock via inventory-service (circuit breaker wraps this) ────
        ProductDto product = inventoryClient.getProduct(productId);

        if (product == null) {
            // Circuit is OPEN — inventory-service is unavailable
            // Save as PENDING so it can be retried later
            log.warn("inventory-service unavailable — saving order as PENDING productId={}", productId);
            Order pending = new Order(productId, quantity, "PENDING", idempotencyKey);
            return orderRepository.save(pending);
        }

        if (product.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(
                String.format("Only %d units of %s available, requested %d",
                    product.getAvailableQuantity(), product.sku(), quantity));
        }

        // ── Reserve stock (circuit breaker wraps this too) ────────────────────
        boolean reserved = inventoryClient.reserveStock(productId, quantity);

        String status = reserved ? "CONFIRMED" : "PENDING";
        Order order = new Order(productId, quantity, status, idempotencyKey);
        Order saved = orderRepository.save(order);

        log.info("Order placed: id={} productId={} quantity={} status={}",
            saved.getId(), productId, quantity, status);
        return saved;
    }

    // ── Read operations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
    }

    // ── Exceptions ────────────────────────────────────────────────────────────

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) { super(message); }
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) { super(message); }
    }
}
