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

Stop inventory-service. Fire requests until the circuit opens — paste all lines at once in Git Bash:

```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}"
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

> **How it works:** `automaticTransitionFromOpenToHalfOpenEnabled: true` is set in
> `application.yml`. Without it, Resilience4j only transitions to HALF-OPEN lazily
> when the next request arrives — the circuit would appear stuck in OPEN.
> With it, a background thread moves the state after 10 seconds regardless.

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

Wait 10 seconds for the next HALF-OPEN window, then confirm the state:

```bash
curl -s http://localhost:8082/actuator/circuitbreakers | jq ".circuitBreakers.\"inventory-service\".state"
# "HALF_OPEN"
```

Now **you** fire the 2 probe calls — these are normal order requests, but in HALF-OPEN state
Resilience4j lets them through to the real inventory-service instead of short-circuiting to
the fallback. If both succeed, the circuit closes. If either fails, it goes back to OPEN.

```bash
# Probe call 1
curl -s -X POST http://localhost:8082/orders \
  -H "Content-Type: application/json" \
  -d "{\"productId\": 1, \"quantity\": 1}" | jq ".status"

# Probe call 2
curl -s -X POST http://localhost:8082/orders \
  -H "Content-Type: application/json" \
  -d "{\"productId\": 1, \"quantity\": 1}" | jq ".status"
```

Both should return `"CONFIRMED"` — inventory-service is back up and the calls succeeded.

Check the circuit closed:

```bash
curl -s http://localhost:8082/actuator/circuitbreakers | jq ".circuitBreakers.\"inventory-service\".state"
# "CLOSED"
```

> **Why exactly 2?** `permittedNumberOfCallsInHalfOpenState: 2` in `application.yml`.
> A 3rd request fired while still in HALF-OPEN would be rejected immediately (fallback fires)
> — Resilience4j won't let more than 2 through until it decides to close or re-open.

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
