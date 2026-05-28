package com.example.inventory.service;

import com.example.inventory.model.Product;
import com.example.inventory.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for inventory management.
 *
 * 12-Factor VI — Stateless processes:
 *   This service holds NO in-memory state between requests.
 *   Every operation reads from and writes to the database.
 *   Any instance of this service can handle any request — safe to scale horizontally.
 *
 * 12-Factor XI — Logs as event streams:
 *   All logging goes to stdout via SLF4J.
 *   No log files. The platform (Kubernetes, ECS) collects and aggregates.
 */
@Service
@Transactional
public class InventoryService {

    // SLF4J logger — writes to stdout (Factor XI)
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ── Read operations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        log.debug("Fetching all products");
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> getInStockProducts() {
        log.debug("Fetching in-stock products");
        return productRepository.findAllInStock();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        log.debug("Fetching product id={}", id);
        return productRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Product not found: id={}", id);
                return new ProductNotFoundException("Product not found: " + id);
            });
    }

    @Transactional(readOnly = true)
    public Product getProductBySku(String sku) {
        log.debug("Fetching product sku={}", sku);
        return productRepository.findBySku(sku)
            .orElseThrow(() -> {
                log.warn("Product not found: sku={}", sku);
                return new ProductNotFoundException("Product not found: " + sku);
            });
    }

    // ── Write operations ──────────────────────────────────────────────────────

    public Product createProduct(Product product) {
        if (productRepository.existsBySku(product.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + product.getSku());
        }
        Product saved = productRepository.save(product);
        log.info("Product created: id={} sku={} stock={}",
            saved.getId(), saved.getSku(), saved.getStockQuantity());
        return saved;
    }

    /**
     * Reserve stock for an order.
     *
     * This is called by order-service via REST — never via direct DB access.
     * That boundary is the bounded context rule from Day 1.
     */
    public Product reserveStock(Long productId, int quantity) {
        log.info("Reserving {} units of product id={}", quantity, productId);
        Product product = getProductById(productId);
        product.reserve(quantity);  // throws if insufficient stock
        Product saved = productRepository.save(product);
        log.info("Reserved {} units of sku={} — available now: {}",
            quantity, saved.getSku(), saved.getAvailableQuantity());
        return saved;
    }

    /**
     * Release a reservation (order cancelled or payment failed).
     */
    public Product releaseStock(Long productId, int quantity) {
        log.info("Releasing {} units of product id={}", quantity, productId);
        Product product = getProductById(productId);
        product.release(quantity);
        Product saved = productRepository.save(product);
        log.info("Released {} units of sku={} — available now: {}",
            quantity, saved.getSku(), saved.getAvailableQuantity());
        return saved;
    }

    // ── Inner exception ───────────────────────────────────────────────────────

    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String message) {
            super(message);
        }
    }
}
