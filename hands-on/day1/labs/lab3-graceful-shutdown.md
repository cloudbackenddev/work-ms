# Lab 3 — Observe Graceful Shutdown (12-Factor IX)

**Time:** 10 minutes  
**Concepts:** 12-Factor IX (disposability), graceful shutdown, Kubernetes SIGTERM

---

## Why this matters

In Kubernetes, pods are killed and restarted constantly — rolling deployments,
node evictions, scaling down. Without graceful shutdown, any in-flight request
at the moment of termination is dropped. The user sees a 500 or a broken response.

With graceful shutdown:
1. Kubernetes sends `SIGTERM`
2. Spring Boot sets readiness to `DOWN` (Kubernetes stops sending new traffic)
3. Spring Boot waits up to 30s for in-flight requests to finish
4. Spring Boot exits cleanly

---

## Step 1 — Verify graceful shutdown is configured

Open `application.yml` and confirm:

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

---

## Step 2 — Simulate a slow request

We'll add a temporary slow endpoint to see graceful shutdown in action.

Open `InventoryController.java` and add this method temporarily:

```java
/**
 * Simulates a slow operation — used to demonstrate graceful shutdown.
 * Remove this before production!
 */
@GetMapping("/slow")
public ResponseEntity<String> slowEndpoint() throws InterruptedException {
    log.info("Slow request started — sleeping for 10 seconds");
    Thread.sleep(10_000);  // simulate 10s of work
    log.info("Slow request completed");
    return ResponseEntity.ok("Completed after 10 seconds");
}
```

Restart the service.

---

## Step 3 — Trigger the slow request, then shut down

**Terminal 1** — start the slow request:
```bash
curl http://localhost:8083/inventory/slow &
```

**Terminal 2** — immediately check readiness, then send SIGTERM:
```bash
# Readiness should be UP
curl http://localhost:8083/actuator/health/readiness

# Find the PID and send SIGTERM (graceful shutdown signal)
# On Mac/Linux:
kill -TERM $(lsof -ti:8083)

# On Windows, stop the process in the terminal running mvn spring-boot:run
# with Ctrl+C
```

**Watch Terminal 1** — the slow request should still complete before the service exits.

**Watch the service logs** — you should see:
```
Commencing graceful shutdown. Waiting for active requests to complete
Active requests have completed. Proceeding with shutdown
```

---

## Step 4 — Check readiness during shutdown

Add a second terminal watching readiness:

```bash
# Poll readiness every second
while true; do
  curl -s http://localhost:8083/actuator/health/readiness | jq .
  sleep 1
done
```

After SIGTERM, readiness should flip to `DOWN` while the slow request is still running.
This is what tells Kubernetes to stop routing new traffic to this pod.

---

## Step 5 — Clean up

Remove the `/slow` endpoint from `InventoryController.java`.

---

## Discussion questions

1. What happens to a user's checkout if the payment service pod is killed mid-request without graceful shutdown?
2. Why does Spring Boot set readiness to `DOWN` *before* waiting for requests to finish?
3. What is the difference between liveness and readiness probes?
   - **Liveness:** Is the process alive? If not, Kubernetes restarts it.
   - **Readiness:** Is the service ready to receive traffic? If not, Kubernetes stops routing to it.

---

## ✅ Done when

- [ ] You observed the slow request complete after SIGTERM
- [ ] You saw readiness flip to DOWN during shutdown
- [ ] You removed the `/slow` endpoint
