# Lab 2 — Externalise Config (12-Factor III)

**Time:** 10 minutes  
**Concepts:** 12-Factor III (config in environment), Spring profiles

---

## The problem this solves

`application.yml` uses `${VAR:default}` syntax throughout:

```yaml
datasource:
  url: ${DB_URL:jdbc:h2:mem:inventorydb;DB_CLOSE_DELAY=-1}
server:
  port: ${SERVER_PORT:8083}
```

This means: read from the environment variable, fall back to the default if not set.
In production you set `DB_URL=jdbc:postgresql://prod-db:5432/inventory` — the code never changes.

---

## Step 1 — Override the port

Stop the service (Ctrl+C), then restart with a different port:

```bash
SERVER_PORT=9090 mvn spring-boot:run
```

**Expected log line on startup:**
```
Tomcat started on port 9090
```

```bash
curl -s http://localhost:9090/actuator/health | jq ".status"
# "UP"
```

Stop and restart on the default port before continuing:
```bash
mvn spring-boot:run
```

---

## Step 2 — Change log level at runtime (no restart)

The service starts at `DEBUG`. In the service terminal you see:
```
DEBUG [inventory-service] c.e.i.service.InventoryService - Fetching product id=1
```

**Silence the logs — change to WARN:**

```bash
curl -s http://localhost:8083/actuator/loggers/com.example.inventory | jq .
```
Expected: `"configuredLevel": "DEBUG"`

```bash
curl -X POST http://localhost:8083/actuator/loggers/com.example.inventory -H "Content-Type: application/json" -d "{\"configuredLevel\": \"WARN\"}"
```

Now hit the API and watch the service terminal:
```bash
curl -s http://localhost:8083/inventory/1 | jq .
```

**Expected:** No DEBUG lines appear in the service terminal. The API still responds normally.

**Restore DEBUG:**
```bash
curl -X POST http://localhost:8083/actuator/loggers/com.example.inventory -H "Content-Type: application/json" -d "{\"configuredLevel\": \"DEBUG\"}"
```

Hit the API again — DEBUG lines return. Zero restarts throughout.

---

## Step 3 — Add a custom info property

Open `application.yml` and add one line under `info.app`:

```yaml
info:
  app:
    name: inventory-service
    version: 1.0.0
    environment: ${APP_ENV:local}   # ← add this
```

Restart with an env var:

```bash
APP_ENV=staging mvn spring-boot:run
```

```bash
curl -s http://localhost:8083/actuator/info | jq ".app.environment"
# "staging"
```

Restart without it:
```bash
mvn spring-boot:run
curl -s http://localhost:8083/actuator/info | jq ".app.environment"
# "local"  ← falls back to the default
```

---

## Step 4 — What NOT to do

Open `InventoryController.java`. There are no hardcoded service URLs anywhere.
If this service needed to call `order-service`:

```java
// ❌ Hardcoded — breaks in every environment except your laptop
String url = "http://localhost:8082";

// ✅ Factor III — read from environment, safe default for local dev
@Value("${ORDER_SERVICE_URL:http://localhost:8082}")
private String orderServiceUrl;
```

---

## Discussion questions

1. What breaks if you hardcode `http://localhost:8082` and deploy to Kubernetes?
2. How would you set `DB_URL` for a pod in Kubernetes? (Hint: ConfigMap vs Secret — which one?)
3. Why does Factor III prefer environment variables over config files checked into git?

---

## ✅ Done when

- [ ] Service starts on port 9090 via `SERVER_PORT` env var
- [ ] Setting log level to `WARN` silences DEBUG output without a restart
- [ ] `/actuator/info` shows `"staging"` when `APP_ENV=staging` is set
