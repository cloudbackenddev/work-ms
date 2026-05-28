# Day 2 Hands-On: Distributed Computing in Practice

## What you'll build

You'll extend the two services from Day 1 (`inventory-service` + `order-service`)
to handle the realities of distributed systems covered in Day 2.

## Prerequisites

- Day 1 Lab 5 completed — both services run and communicate
- `inventory-service` on port 8083, `order-service` on port 8082

## Lab map

| Lab | Topic | Slide reference |
|-----|-------|----------------|
| Lab 1 | Observe cascading failure (Scenario A) | Slide 5 |
| Lab 2 | Add timeout + circuit breaker to order-service | Slides 7–9 |
| Lab 3 | Observe circuit breaker states | Slide 8 |
| Lab 4 | Add structured logging with traceId | Slides 17–18 |
| Lab 5 | Idempotent reserve endpoint | Slide 15 |

## Project structure

```
hands-on/day2/
├── README.md
├── order-service/          ← extended from Day 1 Lab 5
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/order/
│       │   ├── OrderApplication.java
│       │   ├── client/InventoryClient.java   ← adds timeout + circuit breaker
│       │   ├── controller/OrderController.java
│       │   ├── model/Order.java
│       │   ├── repository/OrderRepository.java
│       │   └── service/OrderService.java
│       └── resources/
│           └── application.yml              ← resilience4j config
└── labs/
    ├── lab1-cascading-failure.md
    ├── lab2-circuit-breaker.md
    ├── lab3-observe-states.md
    ├── lab4-structured-logging.md
    └── lab5-idempotency.md
```
