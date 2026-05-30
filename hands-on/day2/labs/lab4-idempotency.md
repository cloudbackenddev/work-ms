# Lab 5 — Idempotent Order Endpoint

**Time:** 15 minutes  
**Concepts:** Day 2 Slide 15 — Idempotency, safe retries, duplicate prevention

---

## The problem

Retries are everywhere — Resilience4j retries, client retries, network retries.
Without idempotency, a retry creates a duplicate order and charges the customer twice.

---

## Step 1 — Demonstrate the problem (without idempotency key)

Place the same order twice with no idempotency key:

```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}"
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}"
```

List orders:
```bash
curl -s http://localhost:8082/orders | jq ".[].id"
```

**Expected:** Two separate orders created. Two stock reservations made.
Check inventory:
```bash
curl -s http://localhost:8083/inventory/1 | jq "{stockQuantity, reservedQuantity}"
```

`reservedQuantity` is 2 — both orders reserved stock independently.

---

## Step 2 — Use an idempotency key

Generate a UUID (the client's responsibility):

```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -H "Idempotency-Key: order-abc-123" -d "{\"productId\": 2, \"quantity\": 1}"
```

Note the order `id` returned.

Send the exact same request again (simulating a retry):
```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -H "Idempotency-Key: order-abc-123" -d "{\"productId\": 2, \"quantity\": 1}"
```

**Expected:** Same order `id` returned. No new order created.

**Check the order-service log:**
```
INFO  [order-service] c.e.o.service.OrderService - Duplicate request — returning existing order id=4 idempotencyKey=order-abc-123
```

**Check inventory — reservedQuantity should only increase by 1, not 2:**
```bash
curl -s http://localhost:8083/inventory/2 | jq "{stockQuantity, reservedQuantity}"
```

---

## Step 3 — Verify a different key creates a new order

```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -H "Idempotency-Key: order-xyz-999" -d "{\"productId\": 2, \"quantity\": 1}"
```

**Expected:** New order with a different `id`. This is a genuinely new request.

---

## Step 4 — Look at the implementation

Open `OrderService.java` and find the idempotency check:

```java
if (idempotencyKey != null) {
    Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        log.info("Duplicate request — returning existing order id={}", existing.get().getId());
        return existing.get();
    }
}
```

And in `Order.java`:
```java
@Column(name = "idempotency_key", unique = true)
private String idempotencyKey;
```

The `unique = true` constraint is the safety net — even if two requests arrive
simultaneously before either has saved, the DB will reject the second INSERT.

---

## Step 5 — Simulate a concurrent duplicate

This is harder to demo manually, but understand the race condition:

1. Request A arrives with key `order-abc-123`
2. Request B arrives with the same key before A has saved
3. Both pass the `findByIdempotencyKey` check (both see nothing)
4. Both try to INSERT
5. The DB unique constraint rejects one — it throws a `DataIntegrityViolationException`
6. The rejected request should retry the lookup and return the existing order

This is why the DB constraint is essential — the application-level check alone is not enough.

---

## Discussion

- Stripe requires an `Idempotency-Key` on every mutation. Why?
- What should happen if the same key is sent with different request bodies?
  (The answer: return the original result, ignore the new body.)
- Is `GET /orders/{id}` idempotent? What about `DELETE /orders/{id}`?

---

## ✅ Done when

- [ ] Two requests without a key create two orders
- [ ] Two requests with the same key return the same order
- [ ] inventory reservedQuantity only increases once for the idempotent pair
- [ ] A different key creates a new order
