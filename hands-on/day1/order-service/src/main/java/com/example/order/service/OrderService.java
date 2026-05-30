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

    /**
     * Place an order.
     *
     * Day 1 version — no resilience:
     *   1. Call inventory-service to check stock (blocks the thread until it responds)
     *   2. Reserve stock
     *   3. Save the order
     *
     * If inventory-service is slow, this thread is stuck at step 1.
     * Fire enough concurrent requests and all Tomcat threads are stuck.
     * That's the cascading failure Lab 1 demonstrates.
     */
    public Order placeOrder(Long productId, int quantity) {
        log.info("Placing order: productId={} quantity={}", productId, quantity);

        // This call blocks the thread — no timeout, no circuit breaker
        ProductDto product = inventoryClient.getProduct(productId);

        if (product.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(
                String.format("Only %d units of '%s' available, requested %d",
                    product.getAvailableQuantity(), product.sku(), quantity));
        }

        inventoryClient.reserveStock(productId, quantity);

        Order order = new Order(productId, quantity, "CONFIRMED");
        Order saved = orderRepository.save(order);
        log.info("Order confirmed: id={} productId={} quantity={}", saved.getId(), productId, quantity);
        return saved;
    }

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
        public InsufficientStockException(String msg) { super(msg); }
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String msg) { super(msg); }
    }
}
