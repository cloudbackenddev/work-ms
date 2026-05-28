# Lab 4 — Structured Logging with TraceId

**Time:** 15 minutes  
**Concepts:** Day 2 Slides 17–18 — Observability, structured logging, traceId correlation

---

## What you'll see

The Day 2 order-service includes Micrometer Tracing. Every request gets a
`traceId` and `spanId` automatically injected into the log pattern.

When order-service calls inventory-service, both services log the same `traceId` —
so you can correlate a single user request across two services.

---

## Step 1 — Observe the log pattern

Look at `application.yml` in the Day 2 order-service:

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [${spring.application.name},%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n"
```

The `%X{traceId:-}` and `%X{spanId:-}` are MDC (Mapped Diagnostic Context) values
that Micrometer Tracing populates automatically for every request.

---

## Step 2 — Place an order and capture the traceId

```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 1}"
```

**Look at the order-service terminal.** You'll see log lines like:

```
2026-05-28 10:30:15 [http-nio-8082-exec-1] INFO  [order-service,4bf92f3577b34da6,00f067aa0ba902b7] c.e.o.service.OrderService - Order placed: id=3 productId=1 quantity=1 status=CONFIRMED
```

The second field in brackets is the **traceId**: `4bf92f3577b34da6`
The third field is the **spanId**: `00f067aa0ba902b7`

---

## Step 3 — Add traceId logging to inventory-service

Open `hands-on/day1/inventory-service/src/main/resources/application.yml`
and update the log pattern:

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [${spring.application.name},%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n"
```

Add the Micrometer Tracing dependency to `hands-on/day1/inventory-service/pom.xml`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
```

Restart inventory-service.

---

## Step 4 — Correlate a request across both services

Place an order:
```bash
curl -s -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\": 2, \"quantity\": 1}"
```

**In the order-service terminal**, find the traceId from the log line.

**In the inventory-service terminal**, search for the same traceId.

You should see the same traceId appear in both services for the same request —
this is how you trace a single user action across a distributed system.

---

## Step 5 — What this looks like in production

In production, both services write JSON logs to stdout.
A log aggregator (ELK, Loki, CloudWatch) collects them.
You search by traceId and see the full journey:

```
traceId=4bf92f3577b34da6

order-service    10:30:15.001  Place order request: productId=2 quantity=1
order-service    10:30:15.003  GET http://localhost:8083/inventory/2
inventory-service 10:30:15.008  Fetching product id=2
inventory-service 10:30:15.012  (DB query)
order-service    10:30:15.015  POST http://localhost:8083/inventory/2/reserve
inventory-service 10:30:15.019  Reserved 1 units of sku=SKU-KB-002
order-service    10:30:15.022  Order placed: id=3 status=CONFIRMED
```

One traceId. Full picture. No guessing.

---

## Discussion

- What's the difference between a traceId and a spanId?
- Why does the log pattern use `%X{traceId:-}` with a `-` default?
- In the slides, the waterfall diagram showed inventory-db taking 105ms.
  How would you see that in logs vs. in a trace UI like Zipkin?

---

## ✅ Done when

- [ ] order-service logs show traceId and spanId on every request
- [ ] inventory-service logs show the same traceId for the same request
- [ ] You can manually correlate a single order across both service logs
