package com.example.order.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Order entity — owned exclusively by order-service.
 *
 * Stores productId as a plain Long — NOT a @ManyToOne relationship.
 * A JPA relationship would create a cross-service DB dependency,
 * violating the bounded context rule from Day 1.
 *
 * Day 2 addition: idempotencyKey field for Lab 5.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String status;   // CONFIRMED, PENDING, FAILED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /**
     * Lab 5 — Idempotency.
     * Stores the client-supplied idempotency key so duplicate requests
     * return the same result without creating a second order.
     */
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    protected Order() {}

    public Order(Long productId, int quantity, String status, String idempotencyKey) {
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setStatus(String status) { this.status = status; }
}
