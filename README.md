# Java Microservices — Entry-Level Engineering Track

A two-day course for entry-level Java engineers learning Spring Boot and
cloud-native microservice design. Includes slide decks, hands-on labs,
and offline assignments.

---

## Course structure

| Day | Topic | Slides | Duration |
|-----|-------|--------|----------|
| Day 1 | Microservice Basics & Cloud-Native Principles | `microservices-day1.html` | 45 min |
| Day 2 | Distributed Computing Challenges | `microservices-day2.html` | 90 min |

Open the HTML files directly in a browser. Navigate with arrow keys or Space.
Press `E` to enter inline edit mode.

---

## Repository layout

```
├── microservices-day1.html     # Slide deck — Day 1 (21 slides)
├── microservices-day2.html     # Slide deck — Day 2 (22 slides)
│
├── hands-on/
│   ├── day1/                   # In-class labs — Day 1
│   │   ├── README.md           # Lab overview and prerequisites
│   │   ├── inventory-service/  # Working Spring Boot project
│   │   │   ├── pom.xml
│   │   │   └── src/
│   │   └── labs/
│   │       ├── lab1-run-and-actuator.md
│   │       ├── lab2-externalise-config.md
│   │       ├── lab3-graceful-shutdown.md
│   │       ├── lab4-custom-health.md
│   │       └── lab5-order-service.md
│   │
│   └── day2/                   # In-class labs — Day 2
│       ├── README.md
│       ├── order-service/      # Extended order-service with Resilience4j
│       │   ├── pom.xml
│       │   └── src/
│       └── labs/
│           ├── lab1-cascading-failure.md
│           ├── lab2-circuit-breaker.md
│           ├── lab3-observe-states.md
│           └── lab4-idempotency.md
│
└── exercises/
    ├── day1/
    │   └── assignment.md       # Offline assignment — Day 1
    └── day2/
        └── assignment.md       # Offline assignment — Day 2
```

---

## Day 1 — Microservice Basics & Cloud-Native Principles

**Slides cover:** Monolith vs. microservices · Bounded contexts · 12-Factor App ·
Cloud-native characteristics · Spring Boot runtime · Spring Boot Actuator ·
E-commerce running example

**Hands-on builds:** `inventory-service` — a fully working Spring Boot microservice
demonstrating 12-Factor config, Actuator health endpoints, graceful shutdown,
and service-to-service REST calls.

| Lab | What you do | Concept |
|-----|-------------|---------|
| Lab 1 | Run the service, explore `/actuator/*` | Actuator, health indicators |
| Lab 2 | Override config via env vars, change log level at runtime | 12-Factor III |
| Lab 3 | Observe graceful shutdown with in-flight requests | 12-Factor IX |
| Lab 4 | Drain stock, watch readiness flip DOWN | Custom health indicators |
| Lab 5 | Build `order-service`, call inventory via REST | Bounded contexts |

**Prerequisites:** Java 17+, Maven 3.8+, [jq](https://jqlang.github.io/jq/)

---

## Day 2 — Distributed Computing Challenges

**Slides cover:** Fallacies of distributed computing · Failure modes ·
Timeout / Retry / Circuit Breaker · Bulkhead · Distributed data · Saga pattern ·
Idempotency · Observability (logs, metrics, tracing) · CAP theorem

**Hands-on builds:** Extended `order-service` with Resilience4j circuit breakers,
structured logging with traceId propagation, and idempotent order placement.
Requires `inventory-service` from Day 1.

| Lab | What you do | Concept |
|-----|-------------|---------|
| Lab 1 | Deliberately slow inventory-service, watch threads pile up | Cascading failure |
| Lab 2 | Add circuit breaker, watch it open and recover | Resilience4j |
| Lab 3 | Walk all three CB states via Actuator | CLOSED → OPEN → HALF-OPEN |
| Lab 4 | Correlate a request across two services by traceId | Structured logging |
| Lab 5 | Send duplicate orders with an idempotency key | Idempotency |

**Prerequisites:** Day 1 Lab 5 completed (both services running)

---

## Running the services

**Day 1 — inventory-service (port 8083):**
```bash
cd hands-on/day1/inventory-service
mvn spring-boot:run
```

**Day 2 — order-service (port 8082):**
```bash
cd hands-on/day2/order-service
mvn spring-boot:run
```

Quick health check:
```bash
curl -s http://localhost:8083/actuator/health | jq ".status"
curl -s http://localhost:8082/actuator/health | jq ".status"
```

---

## Tools required

| Tool | Version | Install |
|------|---------|---------|
| Java | 17+ | [adoptium.net](https://adoptium.net) |
| Maven | 3.8+ | [maven.apache.org](https://maven.apache.org) |
| jq | any | `choco install jq` / `brew install jq` |
| curl | any | included on Windows 10+ |

---

## For instructors

Assignment answer keys are in `exercises/day*/assignment-answers.md`.
These files are excluded from student-facing git distributions via `.gitignore`.

To share the repo with students, either:
- Remove the answer files before pushing, or
- Use a separate branch without the answer files
