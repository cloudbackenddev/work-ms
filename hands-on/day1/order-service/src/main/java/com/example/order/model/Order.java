package com.example.order.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Order entity — stores only productId (a reference by ID).
 *
 * Key design point: there is NO @ManyToOne Product relationship here.
 * Order lives in its own database (ordersdb). Joining across service
 * databases would violate the bounded context rule.
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
    private String status; // CONFIRMED, PENDING, CANCELLED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Order() {}

    public Order(Long productId, int quantity, String status) {
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }

    public Long getId()           { return id; }
    public Long getProductId()    { return productId; }
    public int getQuantity()      { return quantity; }
    public String getStatus()     { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
}
