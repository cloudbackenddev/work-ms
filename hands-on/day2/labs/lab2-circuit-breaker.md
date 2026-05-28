# Lab 2 — Add Timeout + Circuit Breaker

**Time:** 20 minutes  
**Concepts:** Day 2 Slides 7–9 — Timeout, Retry, Circuit Breaker, Resilience4j

---

## What you'll build

Switch from the Day 1 `order-service` to the Day 2 version which has
Resilience4j wired in. You'll see the circuit breaker protect order-service
when inventory-service is slow or down.

---

## Step 1 — Start the Day 2 order-service

Stop the Day 1 order-service (Ctrl+C in its terminal), then:

```bash
cd hands-on/day2/order-service
mvn spring-boot:run
```

Keep inventory-service running from Day 1 (`hands-on/day1/inventory-service`).

Verify:
```bash
curl -s http://localhost:8082/actuator/health | jq ".components.circuitBreakers"
```

**Expected:** A `circuitBreakers` component showing `inventory-service` in state `CLOSED`.

---

## Step 2 — Place a normal order (circuit CLOSED)

```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 2}"
```

**Expected log in order-service terminal:**
```
DEBUG [order-service] c.e.o.client.InventoryClient - GET http://localhost:8083/inventory/1
INFO  [order-service] c.e.o.service.OrderService - Order placed: id=1 productId=1 quantity=2 status=CONFIRMED
```

---

## Step 3 — Stop inventory-service and watch the circuit open

Stop inventory-service (Ctrl+C).

Now fire 5 requests quickly:
```bash
for /l %i in (1,1,5) do curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}"
```

**Expected:** First few requests retry 3 times each (you'll see retry log lines),
then fail. After the failure threshold (60% of 5 calls), the circuit opens.

**Watch the order-service terminal for:**
```
WARN  [order-service] c.e.o.client.InventoryClient - inventory-service unavailable for productId=1 — circuit open or retries exhausted
INFO  [order-service] c.e.o.service.OrderService - Order placed: id=2 productId=1 quantity=1 status=PENDING
```

Orders are saved as `PENDING` — not lost, not errored. The fallback is working.

---

## Step 4 — Check circuit state

```bash
curl -s http://localhost:8082/actuator/health | jq ".components.circuitBreakers"
```

**Expected:**
```json
{
  "status": "UP",
  "details": {
    "inventory-service": {
      "details": {
        "failureRate": "60.0%",
        "state": "OPEN"
      }
    }
  }
}
```

---

## Step 5 — Observe fast-fail while OPEN

With the circuit OPEN, fire another request:
```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}"
```

**Expected:** Response is immediate (< 100ms). No network call is made.
The fallback fires instantly. Check the log — no `GET http://localhost:8083` line.

This is the point: OPEN state protects order-service's threads.

---

## Step 6 — Restart inventory-service and watch recovery

```bash
cd hands-on/day1/inventory-service
mvn spring-boot:run
```

Wait 10 seconds (the `waitDurationInOpenState` in `application.yml`).

The circuit moves to HALF-OPEN and sends 2 probe calls.

```bash
curl -s http://localhost:8082/actuator/health | jq ".components.circuitBreakers.details"
```

After successful probes, state returns to `CLOSED`.

Place a new order — it should be `CONFIRMED` again:
```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}"
curl -s http://localhost:8082/orders | jq ".[].status"
```

---

## Key config to understand

Open `hands-on/day2/order-service/src/main/resources/application.yml`
and find the `resilience4j` section:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventory-service:
        slidingWindowSize: 5          # evaluate last 5 calls
        failureRateThreshold: 60      # open if 60% fail
        waitDurationInOpenState: 10s  # stay open for 10s
        permittedNumberOfCallsInHalfOpenState: 2  # 2 probe calls
```

Try changing `waitDurationInOpenState: 5s` and repeat the lab.

---

## ✅ Done when

- [ ] Circuit opens after repeated inventory-service failures
- [ ] Orders are saved as PENDING (not lost) when circuit is OPEN
- [ ] Fast-fail response observed while circuit is OPEN
- [ ] Circuit recovers to CLOSED after inventory-service restarts
