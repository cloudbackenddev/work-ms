# Lab 5 — Add a Second Service and Call It

**Time:** 20 minutes  
**Concepts:** Bounded contexts, service-to-service REST calls, database-per-service rule

---

## What you'll build

A minimal `order-service` that calls `inventory-service` to check stock
before creating an order. This demonstrates:

- Two services with separate databases (bounded context rule)
- Service-to-service REST communication
- Config-driven service URLs (Factor III)

---

## Step 1 — Create order-service

Create `hands-on/day1/order-service/` with the same structure as `inventory-service`.

**pom.xml** — same parent, add `spring-boot-starter-web`, `spring-boot-starter-actuator`,
`spring-boot-starter-data-jpa`, `h2`, `spring-boot-starter-validation`.

**application.yml:**

```yaml
spring:
  application:
    name: order-service
  datasource:
    url: ${DB_URL:jdbc:h2:mem:ordersdb;DB_CLOSE_DELAY=-1}
    username: ${DB_USER:sa}
    password: ${DB_PASSWORD:}
  lifecycle:
    timeout-per-shutdown-phase: 30s

server:
  port: ${SERVER_PORT:8082}
  shutdown: graceful

# ── Factor III: inventory-service URL from environment ────────────────────────
inventory:
  service:
    url: ${INVENTORY_SERVICE_URL:http://localhost:8083}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
```

---

## Step 2 — Create the Order entity

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;   // reference by ID only — no JOIN to inventory DB

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String status;    // PENDING, CONFIRMED, CANCELLED

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt = java.time.Instant.now();

    // getters, setters, constructors...
}
```

> **Key point:** `Order` stores `productId` — a reference by ID.
> It does NOT have a `@ManyToOne Product product` JPA relationship.
> That would create a cross-service database join, violating the bounded context rule.

---

## Step 3 — Create an InventoryClient

This is how order-service talks to inventory-service — via HTTP, not via DB.

```java
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;

    public InventoryClient(
            RestTemplate restTemplate,
            @Value("${inventory.service.url}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    /**
     * Check if a product has enough stock.
     * Returns the product details from inventory-service.
     */
    public ProductDto getProduct(Long productId) {
        String url = inventoryServiceUrl + "/inventory/" + productId;
        log.debug("Calling inventory-service: GET {}", url);
        return restTemplate.getForObject(url, ProductDto.class);
    }

    /**
     * Reserve stock in inventory-service.
     * Called when an order is confirmed.
     */
    public void reserveStock(Long productId, int quantity) {
        String url = inventoryServiceUrl + "/inventory/" + productId + "/reserve";
        log.info("Calling inventory-service: POST {} quantity={}", url, quantity);
        restTemplate.postForObject(url,
            Map.of("quantity", quantity), Object.class);
    }

    // Simple DTO — only the fields order-service needs
    public record ProductDto(Long id, String name, String sku,
                             int stockQuantity, int reservedQuantity) {
        public int getAvailableQuantity() {
            return stockQuantity - reservedQuantity;
        }
    }
}
```

---

## Step 4 — Create the OrderService

```java
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public Order placeOrder(Long productId, int quantity) {
        // 1. Check stock via inventory-service REST API (NOT via DB)
        InventoryClient.ProductDto product = inventoryClient.getProduct(productId);

        if (product.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(
                "Only " + product.getAvailableQuantity() + " units available");
        }

        // 2. Reserve stock in inventory-service
        inventoryClient.reserveStock(productId, quantity);

        // 3. Create the order in OUR database
        Order order = new Order();
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus("CONFIRMED");
        return orderRepository.save(order);
    }
}
```

---

## Step 5 — Run both services and test

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

**Terminal 3 — place an order:**
```bash
# Place an order for 2 units of product 1
curl -X POST http://localhost:8082/orders \
     -H "Content-Type: application/json" \
     -d '{"productId": 1, "quantity": 2}'

# Check inventory — reservedQuantity should be 2
curl http://localhost:8083/inventory/1 | jq .

# Try to order more than available stock
curl -X POST http://localhost:8082/orders \
     -H "Content-Type: application/json" \
     -d '{"productId": 1, "quantity": 999}'
# Should return 409 Conflict
```

---

## Step 6 — Verify the bounded context rule

```bash
# order-service has its own database
curl http://localhost:8082/h2-console
# JDBC URL: jdbc:h2:mem:ordersdb

# inventory-service has its own database
curl http://localhost:8083/h2-console
# JDBC URL: jdbc:h2:mem:inventorydb

# The orders table has NO product data — only productId (a reference)
# SELECT * FROM ORDERS;  -- only id, product_id, quantity, status, created_at
```

---

## Discussion questions

1. What happens if inventory-service is down when order-service tries to place an order?
   (This is what Module 4 / Day 2 addresses with circuit breakers.)
2. Why does `Order` store `productId` instead of a `@ManyToOne Product` relationship?
3. If you needed the product name in the order response, how would you get it
   without joining across databases?

---

## ✅ Done when

- [ ] Both services start on their respective ports
- [ ] Placing an order via order-service reserves stock in inventory-service
- [ ] Attempting to over-order returns a 409
- [ ] Each service has its own H2 database with no shared tables
