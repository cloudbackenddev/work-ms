# Lab 1 — Observe Cascading Failure (Scenario A)

**Time:** 15 minutes  
**Concepts:** Day 2 Slide 5 — Failure modes, thread exhaustion, cascading failure

---

## What you'll see

Scenario A from the slides: inventory-service doesn't crash — it just gets slow.
order-service threads pile up waiting. order-service stops responding.

This is the failure that takes down systems at 2am.

---

## Step 1 — Start both services (Day 1 versions, no circuit breaker yet)

**Terminal 1:**
```bash
cd hands-on/day1/inventory-service
mvn spring-boot:run
```

**Terminal 2:**
```bash
cd hands-on/day1/order-service
mvn spring-boot:run
```

Verify both are up:
```bash
curl -s http://localhost:8083/actuator/health | jq ".status"
curl -s http://localhost:8082/actuator/health | jq ".status"
```

---

## Step 2 — Add a slow endpoint to inventory-service

Open `hands-on/day1/inventory-service/src/main/java/com/example/inventory/controller/InventoryController.java`
and add this method:

```java
/**
 * Simulates a slow inventory-service — used for Lab 1 only.
 * Remove after the lab.
 */
@GetMapping("/slow/{id}")
public ResponseEntity<Product> slowGetProduct(@PathVariable Long id)
        throws InterruptedException {
    log.warn("Slow endpoint called for id={} — sleeping 8 seconds", id);
    Thread.sleep(8_000);
    return ResponseEntity.ok(inventoryService.getProductById(id));
}
```

Restart inventory-service.

---

## Step 3 — Point order-service at the slow endpoint

Open `hands-on/day1/order-service/src/main/java/com/example/order/client/InventoryClient.java`
and temporarily change `getProduct` to call `/inventory/slow/{id}` instead of `/inventory/{id}`.

Restart order-service.

---

## Step 4 — Fire multiple concurrent requests

Open a second terminal and run these simultaneously (don't wait for responses):

```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
```

**While those are running**, try a health check:
```bash
curl -s http://localhost:8082/actuator/health | jq ".status"
```

**Expected:** The health check itself may be slow or time out — order-service's
threads are all blocked waiting for inventory-service.

**Watch the order-service terminal** — you'll see all threads stuck on the slow call.

---

## Step 5 — Observe the thread exhaustion in metrics

```bash
curl -s "http://localhost:8082/actuator/metrics/tomcat.threads.busy" | jq ".measurements[0].value"
```

**Expected:** A high number — all Tomcat threads are occupied waiting for inventory-service.

---

## Step 6 — Clean up

- Revert the `getProduct` URL change in `InventoryClient.java`
- Remove the `/slow/{id}` endpoint from `InventoryController.java`
- Restart both services

---

## Discussion

- inventory-service never crashed. It just got slow. Why did order-service fail?
- What would happen to a third service that calls order-service?
- This is exactly what circuit breakers prevent — Lab 2 adds one.

---

## ✅ Done when

- [ ] You observed order-service threads blocking on the slow inventory call
- [ ] The health check was slow or unresponsive during the load
- [ ] You reverted both changes
