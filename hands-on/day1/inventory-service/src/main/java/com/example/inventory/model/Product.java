package com.example.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Product entity — the core domain object of inventory-service.
 *
 * This service owns this table exclusively.
 * No other service may query it directly (12-Factor / bounded context rule).
 * Other services must call inventory-service's REST API.
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String sku;

    @Min(0)
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    // Tracks how many units are reserved (order placed but not yet shipped)
    @Min(0)
    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @NotNull
    @Column(name = "unit_price", nullable = false)
    private double unitPrice;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected Product() {}

    public Product(String name, String sku, int stockQuantity, double unitPrice) {
        this.name = name;
        this.sku = sku;
        this.stockQuantity = stockQuantity;
        this.unitPrice = unitPrice;
    }

    // ── Business logic ────────────────────────────────────────────────────────

    /**
     * Returns the number of units actually available to reserve.
     * available = total stock - already reserved
     */
    public int getAvailableQuantity() {
        return stockQuantity - reservedQuantity;
    }

    /**
     * Reserves units for an order. Throws if insufficient stock.
     * Called by order-service via REST — not directly via DB.
     */
    public void reserve(int quantity) {
        if (quantity > getAvailableQuantity()) {
            throw new IllegalStateException(
                String.format("Cannot reserve %d units of %s — only %d available",
                    quantity, sku, getAvailableQuantity())
            );
        }
        this.reservedQuantity += quantity;
    }

    /**
     * Releases a reservation (e.g. order cancelled).
     */
    public void release(int quantity) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantity);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public int getStockQuantity() { return stockQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public double getUnitPrice() { return unitPrice; }

    public void setName(String name) { this.name = name; }
    public void setSku(String sku) { this.sku = sku; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
