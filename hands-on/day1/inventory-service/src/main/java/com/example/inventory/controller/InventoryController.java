package com.example.inventory.controller;

import com.example.inventory.model.Product;
import com.example.inventory.service.InventoryService;
import com.example.inventory.service.InventoryService.ProductNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for inventory-service.
 *
 * This is the ONLY entry point for other services.
 * order-service must call these endpoints — it may NOT query the DB directly.
 * That is the bounded context boundary enforced at the API layer.
 *
 * 12-Factor VII — Port binding:
 *   Spring Boot's embedded Tomcat serves these endpoints.
 *   No external app server (Tomcat, JBoss) is needed.
 */
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // ── GET /inventory ────────────────────────────────────────────────────────

    /**
     * List all products (including out-of-stock).
     * GET /inventory
     */
    @GetMapping
    public List<Product> getAllProducts() {
        return inventoryService.getAllProducts();
    }

    /**
     * List only products with available stock.
     * GET /inventory?inStock=true
     */
    @GetMapping(params = "inStock=true")
    public List<Product> getInStockProducts() {
        return inventoryService.getInStockProducts();
    }

    // ── GET /inventory/{id} ───────────────────────────────────────────────────

    /**
     * Get a single product by ID.
     * GET /inventory/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getProductById(id));
    }

    /**
     * Get a single product by SKU.
     * GET /inventory/sku/SKU-WH-001
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(inventoryService.getProductBySku(sku));
    }

    // ── POST /inventory ───────────────────────────────────────────────────────

    /**
     * Create a new product.
     * POST /inventory
     * Body: { "name": "...", "sku": "...", "stockQuantity": 100, "unitPrice": 29.99 }
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@Valid @RequestBody Product product) {
        return inventoryService.createProduct(product);
    }

    // ── POST /inventory/{id}/reserve ──────────────────────────────────────────

    /**
     * Reserve stock for an order.
     * Called by order-service when a customer places an order.
     *
     * POST /inventory/1/reserve
     * Body: { "quantity": 2 }
     *
     * Returns 409 Conflict if insufficient stock.
     */
    @PostMapping("/{id}/reserve")
    public ResponseEntity<Product> reserveStock(
            @PathVariable Long id,
            @RequestBody @Valid ReservationRequest request) {

        log.info("Reserve request: productId={} quantity={}", id, request.quantity());
        Product updated = inventoryService.reserveStock(id, request.quantity());
        return ResponseEntity.ok(updated);
    }

    // ── POST /inventory/{id}/release ──────────────────────────────────────────

    /**
     * Release a reservation (order cancelled / payment failed).
     * POST /inventory/1/release
     * Body: { "quantity": 2 }
     */
    @PostMapping("/{id}/release")
    public ResponseEntity<Product> releaseStock(
            @PathVariable Long id,
            @RequestBody @Valid ReservationRequest request) {

        log.info("Release request: productId={} quantity={}", id, request.quantity());
        Product updated = inventoryService.releaseStock(id, request.quantity());
        return ResponseEntity.ok(updated);
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ProductNotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleInsufficientStock(IllegalStateException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }

    // ── Request record ────────────────────────────────────────────────────────

    public record ReservationRequest(@Min(1) int quantity) {}
}
