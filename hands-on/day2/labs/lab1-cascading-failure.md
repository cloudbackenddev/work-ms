# Lab 1 — Observe Cascading Failure (Scenario A)

**Time:** 15 minutes  
**Concepts:** Day 2 Slide 5 — Failure modes, thread exhaustion, cascading failure

---

## What you'll see

Scenario A from the slides: inventory-service doesn't crash — it just gets slow.
order-service threads pile up waiting. order-service stops responding entirely.

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

Wait until you see this line in the order-service log — it confirms the thread metrics are live:
```
Registered Tomcat thread metrics — max pool size: 5
```

Verify both are up:
```bash
curl -s http://localhost:8083/actuator/health | jq ".status"
curl -s http://localhost:8082/actuator/health | jq ".status"
```

Both should return `"UP"`.

> **Note:** The `/slow/{id}` endpoint is already built into inventory-service and
> order-service already calls it. No code changes needed — just start and observe.

---

## Step 2 — Fire 5 concurrent requests

Open a **new terminal (Terminal 3)** and paste all 5 lines at once so they fire simultaneously:

```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}" &
```

Each request calls inventory-service's `/slow/{id}` endpoint which sleeps for 8 seconds.
All 5 Tomcat threads are now blocked waiting.

> **Important:** The metric checks below must be run in a **separate terminal (Terminal 4)**,
> not Terminal 3. Git Bash won't process new commands in Terminal 3 while background jobs are running.

---

## Step 3 — Observe thread exhaustion

Open **Terminal 4** and run immediately (within 8 seconds of firing the requests):

```bash
# Should show 5.0 — all threads occupied
curl -s "http://localhost:8082/actuator/metrics/order.tomcat.threads.busy" | jq ".measurements[0].value"

# Should show 5.0 — the configured maximum
curl -s "http://localhost:8082/actuator/metrics/order.tomcat.threads.max" | jq ".measurements[0].value"
```

**Expected:** Both return `5.0` — the thread pool is fully exhausted.

---

## Step 4 — Observe the health check failing

Still in Terminal 4, while the requests are blocking:

```bash
curl -s --max-time 5 http://localhost:8082/actuator/health
```

**Expected:** The request hangs and times out after 5 seconds, or returns very slowly.

This is the cascading failure: inventory-service never crashed. It just got slow.
But order-service has no free threads — it can't serve anything, including its own health check.
A load balancer would now mark order-service as unhealthy and stop sending traffic to it.

---

## Step 5 — Watch the logs

**In Terminal 2 (order-service)** you'll see 5 lines like:
```
WARN  [order-service] c.e.o.client.InventoryClient - Calling slow inventory endpoint — thread will block for ~8s: GET http://localhost:8083/inventory/slow/1
```
All 5 appear almost simultaneously — 5 threads all stuck at the same time.

**In Terminal 1 (inventory-service)** you'll see:
```
WARN  [inventory-service] c.e.i.controller.InventoryController - Slow endpoint called for id=1 — sleeping 8 seconds
```
5 of these, one per request. inventory-service is alive and logging — it's not crashed, just slow.

---

## Discussion

- inventory-service never crashed. Why did order-service fail?
- What would happen to a third service that calls order-service right now?
- The thread pool is only 5 here to make exhaustion fast. Production pools are larger (200+)
  but the same failure happens — it just takes more concurrent slow calls to trigger.
- This is exactly what circuit breakers prevent — Lab 2 adds one.

---

## ✅ Done when

- [ ] `order.tomcat.threads.busy` showed `5.0` during the load
- [ ] The health check was slow or timed out during the load
- [ ] You understand why a slow dependency is more dangerous than a crashed one
