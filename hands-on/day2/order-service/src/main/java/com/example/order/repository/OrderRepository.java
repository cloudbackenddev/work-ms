package com.example.order.repository;

import com.example.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Lab 5 — find an existing order by idempotency key.
     * If found, return it instead of creating a duplicate.
     */
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
