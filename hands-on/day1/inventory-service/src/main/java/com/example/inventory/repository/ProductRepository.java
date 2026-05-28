package com.example.inventory.repository;

import com.example.inventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Spring Data repository for Product.
 *
 * Spring Boot generates the implementation at startup — no boilerplate needed.
 * This is the ONLY component that touches the database directly.
 * The service layer calls this; the controller calls the service.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    /**
     * Find all products that have at least 1 unit available to reserve.
     */
    @Query("SELECT p FROM Product p WHERE (p.stockQuantity - p.reservedQuantity) > 0")
    List<Product> findAllInStock();

    boolean existsBySku(String sku);
}
