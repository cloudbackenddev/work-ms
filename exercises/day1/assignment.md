# Day 1 — Offline Assignment

**Submit by:** Before Day 2  
**Format:** Code + a short written answer (3–5 sentences) per question  
**Estimated time:** 2–3 hours

---

## Part 1 — Extend inventory-service (Code)

### Exercise 1.1 — Add a `category` field

Add a `category` field to the `Product` entity (e.g. `"Electronics"`, `"Accessories"`).

Requirements:
- The field must be stored in the database
- `GET /inventory?category=Electronics` must return only products in that category
- The field must be required when creating a product (`@NotBlank`)
- Update `data.sql` to include a category for each seed product

### Exercise 1.2 — Add a low-stock health indicator

Modify `StockLevelHealthIndicator` so that:
- Status is `UP` when 3 or more products have available stock
- Status is `WARN` (use `Health.unknown()`) when 1–2 products have available stock
- Status is `DOWN` when 0 products have available stock
- The response always includes the count of in-stock products as a detail

### Exercise 1.3 — Externalise the warn threshold

The `WARN_THRESHOLD = 10` in `StockLevelHealthIndicator` is currently hardcoded.

Move it to `application.yml` as:
```yaml
inventory:
  health:
    low-stock-threshold: 10
```

Inject it using `@Value("${inventory.health.low-stock-threshold:10}")`.

Verify it works by setting the env var `INVENTORY_HEALTH_LOW_STOCK_THRESHOLD=3`
and confirming the threshold changes without a code change.

---

## Part 2 — 12-Factor Audit (Written)

Review the `inventory-service` codebase and answer the following:

### Question 2.1
Which of the 12 factors does `inventory-service` currently satisfy?
For each factor you identify, point to the specific file and line that demonstrates it.

### Question 2.2
Which factors does `inventory-service` NOT yet satisfy, and why?
Pick one unsatisfied factor and describe what you would need to add to satisfy it.

### Question 2.3
`application.yml` has `h2.console.enabled: true`.
Which 12-Factor principle does this violate if left enabled in production?
How would you fix it using Spring profiles?

---

## Part 3 — Design Question (Written)

### Question 3.1 — Service boundary decision

Your team is building an e-commerce platform. A product manager asks:
*"Can the order-service just query the inventory database directly?
It would be much simpler than making a REST call."*

Write a response (3–5 sentences) explaining:
- Why this is a bad idea even though it works technically
- What specific problems it causes as the system grows
- What the correct approach is

### Question 3.2 — Monolith vs. microservice

Your startup has 3 engineers and is building a new product.
A senior engineer suggests starting with a monolith.
Another suggests microservices from day one.

Write 3–5 sentences arguing for the monolith approach,
then 3–5 sentences arguing for microservices.
Which would you choose and why?

---

## Part 4 — Bonus (Optional)

### Exercise 4.1 — Add a `PATCH /inventory/{id}` endpoint

Allow partial updates to a product (e.g. update only the price or stock quantity).
Use `@PatchMapping` and only update fields that are present in the request body.

### Exercise 4.2 — Add a Spring profile for production

Create `application-prod.yml` that:
- Disables the H2 console
- Sets `show-details: never` on the health endpoint
- Sets log level to `WARN` for `com.example.inventory`

Verify it activates with `SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run`.

---

## Submission checklist

- [ ] Exercise 1.1 — `category` field with filtered endpoint
- [ ] Exercise 1.2 — Updated health indicator with WARN state
- [ ] Exercise 1.3 — Externalised threshold via config
- [ ] Question 2.1 — 12-Factor audit with file references
- [ ] Question 2.2 — Unsatisfied factor + fix description
- [ ] Question 2.3 — H2 console + Spring profiles answer
- [ ] Question 3.1 — Service boundary response
- [ ] Question 3.2 — Monolith vs. microservice argument
