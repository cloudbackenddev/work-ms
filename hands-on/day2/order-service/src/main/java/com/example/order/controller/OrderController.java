package com.example.order.controller;

import com.example.order.model.Order;
import com.example.order.service.OrderService;
import com.example.order.service.OrderService.InsufficientStockException;
import com.example.order.service.OrderService.OrderNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for order-service.
 *
 * Lab 5 — Idempotency:
 * POST /orders accepts an optional Idempotency-Key header.
 * If the same key is sent twice, the second request returns the first order.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Place an order.
     *
     * POST /orders
     * Headers: Idempotency-Key: <uuid>   (optional but recommended)
     * Body: { "productId": 1, "quantity": 2 }
     *
     * Returns 201 Created on first call.
     * Returns 200 OK with the same order on duplicate calls (same Idempotency-Key).
     */
    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {

        log.info("Place order request: productId={} quantity={} idempotencyKey={}",
            request.productId(), request.quantity(), idempotencyKey);

        Order order = orderService.placeOrder(request.productId(), request.quantity(), idempotencyKey);

        // 200 if this was a duplicate (idempotent replay), 201 if new
        HttpStatus status = (idempotencyKey != null &&
            order.getIdempotencyKey() != null &&
            order.getCreatedAt().isBefore(java.time.Instant.now().minusSeconds(1)))
            ? HttpStatus.OK
            : HttpStatus.CREATED;

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleInsufficientStock(InsufficientStockException ex) {
        log.warn("Insufficient stock: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(OrderNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    // ── Request record ────────────────────────────────────────────────────────

    public record OrderRequest(
            @NotNull @Min(1) Long productId,
            @Min(1) int quantity) {}
}
