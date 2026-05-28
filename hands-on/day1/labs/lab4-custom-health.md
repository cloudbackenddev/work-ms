# Lab 4 — Custom Health Indicator

**Time:** 15 minutes  
**Concepts:** Actuator health indicators, Kubernetes readiness, cloud-native health-aware

---

## What's already there

Open `StockLevelHealthIndicator.java`. It already implements a custom health check
that reports `DOWN` when no products are in stock.

Run the service and check:

```bash
curl http://localhost:8083/actuator/health | jq .
```

You should see a `stockLevel` component in the response.

---

## Step 1 — Trigger the DOWN state

We'll drain all stock to see the health indicator flip to DOWN.

```bash
# Reserve ALL stock for product 1 (150 units)
curl -X POST http://localhost:8083/inventory/1/reserve \
     -H "Content-Type: application/json" \
     -d '{"quantity": 150}'

# Reserve ALL stock for products 2-5 similarly
curl -X POST http://localhost:8083/inventory/2/reserve -H "Content-Type: application/json" -d '{"quantity": 75}'
curl -X POST http://localhost:8083/inventory/3/reserve -H "Content-Type: application/json" -d '{"quantity": 200}'
curl -X POST http://localhost:8083/inventory/4/reserve -H "Content-Type: application/json" -d '{"quantity": 60}'
curl -X POST http://localhost:8083/inventory/5/reserve -H "Content-Type: application/json" -d '{"quantity": 40}'

# Now check health — stockLevel should be DOWN
curl http://localhost:8083/actuator/health | jq .

# Check readiness specifically
curl http://localhost:8083/actuator/health/readiness | jq .
```

> **What would Kubernetes do?** With readiness `DOWN`, Kubernetes stops routing
> traffic to this pod. New requests go to other healthy pods instead.

---

## Step 2 — Release stock to recover

```bash
# Release stock for product 1
curl -X POST http://localhost:8083/inventory/1/release \
     -H "Content-Type: application/json" \
     -d '{"quantity": 150}'

# Check health again — should recover
curl http://localhost:8083/actuator/health | jq .
```

---

## Step 3 — Write your own health indicator

Add a new health indicator that checks whether the H2 console is accessible.
Create `src/main/java/com/example/inventory/health/H2ConsoleHealthIndicator.java`:

```java
package com.example.inventory.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class H2ConsoleHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // TODO: add a real check — for now just report UP with a detail
        return Health.up()
            .withDetail("h2Console", "http://localhost:8083/h2-console")
            .withDetail("note", "Dev only — disable in production")
            .build();
    }
}
```

Restart and check `/actuator/health` — your new indicator should appear.

---

## Step 4 — Understand the health hierarchy

```bash
# Top-level status (UP/DOWN)
curl http://localhost:8083/actuator/health

# Liveness — is the JVM alive?
curl http://localhost:8083/actuator/health/liveness

# Readiness — is the service ready for traffic?
curl http://localhost:8083/actuator/health/readiness

# Individual component
curl http://localhost:8083/actuator/health/stockLevel
curl http://localhost:8083/actuator/health/db
```

---

## Discussion questions

1. Should `StockLevelHealthIndicator` affect liveness or readiness? Why?
   (Hint: liveness = "should Kubernetes restart me?", readiness = "should Kubernetes send me traffic?")
2. What's the risk of a health indicator that makes a slow external call?
3. In production, would you use `show-details: always`? What are the security implications?

---

## ✅ Done when

- [ ] You triggered the `stockLevel` DOWN state by draining all stock
- [ ] You saw readiness flip to DOWN
- [ ] You recovered by releasing stock
- [ ] You added your own health indicator and saw it in `/actuator/health`
