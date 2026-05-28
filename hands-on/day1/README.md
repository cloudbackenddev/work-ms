# Day 1 Hands-On: Your First Cloud-Native Microservice

## What you'll build

A working `inventory-service` — a Spring Boot microservice that demonstrates every concept from Day 1:

| Concept | Where you'll see it |
|---|---|
| Single-responsibility service | `inventory-service` owns only stock data |
| 12-Factor III — Config in environment | DB URL, port via env vars / `application.yml` |
| 12-Factor VI — Stateless processes | No in-memory state; all data in H2 |
| 12-Factor VII — Port binding | Embedded Tomcat on configurable port |
| 12-Factor IX — Disposability | Graceful shutdown configured |
| 12-Factor XI — Logs | SLF4J → stdout (no file appenders) |
| Cloud-native: Health-aware | `/actuator/health` with DB health indicator |
| Cloud-native: Observable | `/actuator/metrics`, `/actuator/info` |
| Actuator probes | Liveness + readiness endpoints |

## Prerequisites

- Java 17+
- Maven 3.8+
- Your favourite IDE (IntelliJ / VS Code)

## Project structure

```
inventory-service/
├── pom.xml
└── src/
    └── main/
        ├── java/com/example/inventory/
        │   ├── InventoryApplication.java       ← entry point
        │   ├── controller/
        │   │   └── InventoryController.java    ← REST endpoints
        │   ├── model/
        │   │   └── Product.java                ← JPA entity
        │   ├── repository/
        │   │   └── ProductRepository.java      ← Spring Data
        │   └── service/
        │       └── InventoryService.java        ← business logic
        └── resources/
            ├── application.yml                 ← 12-Factor config
            └── data.sql                        ← seed data
```

## Lab steps

### Lab 1 — Run the service and explore Actuator (15 min)
See: `labs/lab1-run-and-actuator.md`

### Lab 2 — Externalise config (Factor III) (10 min)
See: `labs/lab2-externalise-config.md`

### Lab 3 — Observe graceful shutdown (Factor IX) (10 min)
See: `labs/lab3-graceful-shutdown.md`

### Lab 4 — Add a custom health indicator (15 min)
See: `labs/lab4-custom-health.md`

### Lab 5 — Add a second service and call it (20 min)
See: `labs/lab5-order-service.md`

---

> **Tip:** Each lab builds on the previous one. Work through them in order.
> The `solutions/` folder contains completed code for each lab if you get stuck.
