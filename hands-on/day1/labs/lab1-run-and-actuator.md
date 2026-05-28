# Lab 1 — Run the service and explore Actuator

**Time:** 15 minutes  
**Concepts:** 12-Factor VII (port binding), Actuator endpoints, health indicators

---

## Prerequisites — install jq

`jq` is a lightweight command-line JSON processor. We use it to pretty-print
and query API responses. It has no runtime dependencies and is available everywhere.

```bash
# macOS
brew install jq

# Ubuntu / Debian
sudo apt-get install jq

# Windows (Chocolatey)
choco install jq

# Windows (Scoop)
scoop install jq

# Verify
jq --version
```

> **Why jq and not `python -m json.tool`?**
> `jq` is purpose-built for JSON, needs no language runtime, and lets you
> do useful things like `| jq ".components.db"` to drill into a specific field.
> You'll use it constantly when working with microservice APIs.

---

## Step 1 — Build and run

```bash
cd hands-on/day1/inventory-service
mvn spring-boot:run
```

You should see Spring Boot start on port **8083** with output like:

```
Started InventoryApplication in 2.3 seconds (JVM running for 2.8)
```

> **What just happened?** Spring Boot started an embedded Tomcat server.
> There is no external app server. The JAR *is* the service. This is 12-Factor VII.

---

## Step 2 — Hit the health endpoint

```bash
# Pretty-print the full health response
curl -s http://localhost:8083/actuator/health | jq .

# Just the top-level status
curl -s http://localhost:8083/actuator/health | jq ".status"

# Just the database component
curl -s http://localhost:8083/actuator/health | jq ".components.db"

# Just the custom stock level indicator
curl -s http://localhost:8083/actuator/health | jq ".components.stockLevel"
```

Expected response:

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": { "database": "H2", "validationQuery": "isValid()" }
    },
    "diskSpace": { "status": "UP", ... },
    "stockLevel": {
      "status": "UP",
      "details": { "inStockProducts": 5 }
    }
  }
}
```

> **Notice:** `stockLevel` is a *custom* health indicator (see `StockLevelHealthIndicator.java`).
> Kubernetes uses this endpoint to decide whether to send traffic to this pod.

---

## Step 3 — Explore the other endpoints

```bash
# What info does the service report about itself?
curl -s http://localhost:8083/actuator/info | jq .

# What metrics are available? (returns a list of metric names)
curl -s http://localhost:8083/actuator/metrics | jq ".names[]"

# Drill into a specific metric — HTTP request count
curl -s "http://localhost:8083/actuator/metrics/http.server.requests" | jq .

# Just the measurement values
curl -s "http://localhost:8083/actuator/metrics/http.server.requests" | jq ".measurements"

# What environment variables and config properties are active?
curl -s http://localhost:8083/actuator/env | jq .

# Just the active profiles
curl -s http://localhost:8083/actuator/env | jq ".activeProfiles"
```

---

## Step 4 — Call the inventory API

```bash
# List all products
curl -s http://localhost:8083/inventory | jq .

# Get a single product — just the name and available quantity
curl -s http://localhost:8083/inventory/1 | jq "{name, sku, available: (.stockQuantity - .reservedQuantity)}"

# Get only in-stock products
curl -s "http://localhost:8083/inventory?inStock=true" | jq .

# Reserve 3 units of product 1
curl -s -X POST http://localhost:8083/inventory/1/reserve \
     -H "Content-Type: application/json" \
     -d '{"quantity": 3}' | jq .

# Check the product again — reservedQuantity should be 3
curl -s http://localhost:8083/inventory/1 | jq "{id, sku, stockQuantity, reservedQuantity}"
```

---

## Step 5 — Browse the H2 console

Open http://localhost:8083/h2-console in your browser.

> **Important:** The browser will pre-fill a wrong JDBC URL (`jdbc:h2:~/test`).
> You must **clear it** and type the correct one manually.

| Field | Value |
|-------|-------|
| JDBC URL | `jdbc:h2:mem:inventorydb` |
| Username | `sa` |
| Password | *(leave blank)* |

Click **Connect**, then run:

```sql
SELECT * FROM PRODUCT;
```

You should see the 5 seed products from `data.sql`.

---

## Discussion questions

1. Where is the database URL configured? What happens if you don't set `DB_URL`?
2. What does `show-details: always` do in `application.yml`? Would you use this in production?
3. The `stockLevel` health indicator returns `DOWN` when no products are in stock. What would Kubernetes do if this happened?

---

## ✅ Done when

- [ ] Service starts on port 8083
- [ ] `/actuator/health` returns `UP` with `stockLevel` component
- [ ] You can list products and reserve stock via the API
- [ ] You can see the data in the H2 console
