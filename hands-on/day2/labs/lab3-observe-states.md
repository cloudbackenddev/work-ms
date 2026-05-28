# Lab 3 — Observe Circuit Breaker States

**Time:** 15 minutes  
**Concepts:** Day 2 Slide 8 — CLOSED → OPEN → HALF-OPEN → CLOSED state machine

---

## What you'll observe

Walk through all three circuit breaker states deliberately,
watching the Actuator endpoint and logs at each transition.

---

## Step 1 — Confirm CLOSED state

Both services running. Circuit should be CLOSED.

```bash
curl -s http://localhost:8082/actuator/circuitbreakers | jq ".circuitBreakers.\"inventory-service\".state"
# "CLOSED"

curl -s http://localhost:8082/actuator/circuitbreakerevents | jq ".circuitBreakerEvents[-3:] | .[].type"
# Recent events — should show SUCCESS
```

---

## Step 2 — Force failures to open the circuit

Stop inventory-service. Fire requests until the circuit opens:

```bash
for /l %i in (1,1,6) do curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" & echo Request %i sent
```

Check state:
```bash
curl -s http://localhost:8082/actuator/circuitbreakers | jq ".circuitBreakers.\"inventory-service\""
```

**Expected output:**
```json
{
  "failureRate": "60.0%",
  "slowCallRate": "0.0%",
  "failedCalls": 3,
  "successfulCalls": 2,
  "state": "OPEN"
}
```

Check the event log — you'll see the exact moment the circuit opened:
```bash
curl -s http://localhost:8082/actuator/circuitbreakerevents | jq ".circuitBreakerEvents[-5:] | .[] | {type, creationTime}"
```

Look for `"type": "STATE_TRANSITION"` — that's the circuit opening.

---

## Step 3 — Observe HALF-OPEN

Wait 10 seconds (the `waitDurationInOpenState`). The circuit moves to HALF-OPEN automatically.

```bash
curl -s http://localhost:8082/actuator/circuitbreakers | jq ".circuitBreakers.\"inventory-service\".state"
# "HALF_OPEN"
```

In HALF-OPEN, only 2 probe calls are allowed through (`permittedNumberOfCallsInHalfOpenState: 2`).
inventory-service is still down, so the probes fail and the circuit goes back to OPEN.

```bash
curl -s http://localhost:8082/actuator/circuitbreakerevents | jq ".circuitBreakerEvents[-3:] | .[] | {type, state: .stateTransition}"
```

---

## Step 4 — Restart inventory-service and observe recovery

```bash
cd hands-on/day1/inventory-service
mvn spring-boot:run
```

Wait 10 seconds for the next HALF-OPEN window.

This time the 2 probe calls succeed. The circuit closes.

```bash
curl -s http://localhost:8082/actuator/circuitbreakers | jq ".circuitBreakers.\"inventory-service\".state"
# "CLOSED"
```

Place an order — it should be CONFIRMED:
```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" | jq ".status"
# "CONFIRMED"
```

---

## Step 5 — Check PENDING orders

During the OPEN period, orders were saved as PENDING. List them:

```bash
curl -s http://localhost:8082/orders | jq ".[] | {id, status, productId}"
```

In a real system, a background job would retry PENDING orders.
That's a Saga compensating transaction — covered in the slides.

---

## ✅ Done when

- [ ] You observed all three states: CLOSED → OPEN → HALF-OPEN → CLOSED
- [ ] You used `/actuator/circuitbreakerevents` to see the state transitions
- [ ] PENDING orders exist from the OPEN period
- [ ] New orders are CONFIRMED after recovery
