# 🏗️ Architecture Design — Phỏng vấn Mid 3+ năm

---

## 1. SOLID Principles

### S — Single Responsibility Principle
> Một class chỉ có **một lý do để thay đổi**

```java
// Bad: class làm quá nhiều việc
class UserManager {
    public void saveUser(User u) { /* DB logic */ }
    public void sendEmail(User u) { /* Email logic */ }
    public String report(User u) { /* Report logic */ }
}
// Good: tách ra theo trách nhiệm
class UserRepository { void save(User u) {} }
class EmailService    { void sendWelcome(User u) {} }
class UserReport      { String generate(User u) {} }
```

### O — Open/Closed Principle
> Mở để **mở rộng**, đóng để **sửa đổi**

```java
// Bad: thêm loại shape → phải sửa AreaCalculator
class AreaCalculator {
    double calculate(Object shape) {
        if (shape instanceof Circle) { /* ... */ }
        else if (shape instanceof Rectangle) { /* ... */ }
        // thêm Triangle phải sửa đây!
    }
}
// Good: thêm shape mà không sửa calculator
interface Shape { double area(); }
class Circle    implements Shape { public double area() { return Math.PI * r * r; } }
class Rectangle implements Shape { public double area() { return w * h; } }
class AreaCalculator { double calculate(Shape s) { return s.area(); } }
```

### L — Liskov Substitution Principle
> Subclass phải **thay thế được** superclass không vỡ behavior

```java
// Bad: Square extends Rectangle nhưng break contract setWidth/setHeight
class Square extends Rectangle {
    void setWidth(int w) {
        super.setWidth(w);
        super.setHeight(w); // thay đổi cả height — vi phạm LSP!
    }
}
// Fix: dùng composition hoặc interface thay vì inheritance
```

### I — Interface Segregation Principle
> Client không bị ép implement interface mà họ không dùng

```java
// Bad: Robot phải implement eat() và sleep()
interface Worker { void work(); void eat(); void sleep(); }
// Good
interface Workable { void work(); }
interface Eatable  { void eat();  }
class Robot implements Workable { public void work() {} }
class Human implements Workable, Eatable { public void work() {} public void eat() {} }
```

### D — Dependency Inversion Principle
> Depend on **abstractions**, not concretions

```java
// Bad: phụ thuộc vào MySQL cụ thể
class OrderService {
    private MySQLOrderRepository repo = new MySQLOrderRepository();
}
// Good: inject interface, dễ swap / test
class OrderService {
    private final OrderRepository repo; // interface
    OrderService(OrderRepository repo) { this.repo = repo; }
}
```

---

## 2. Design Patterns

### Creational

#### Singleton — Thread-safe
```java
// Double-checked locking với volatile
public class Config {
    private static volatile Config instance;
    private Config() {}
    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) instance = new Config();
            }
        }
        return instance;
    }
}
// Cách tốt nhất: dùng enum (serialization-safe, thread-safe)
public enum Config { INSTANCE; }
```

#### Factory Method
```java
interface Notification { void send(String msg); }
class NotificationFactory {
    public static Notification create(String type) {
        return switch (type) {
            case "EMAIL" -> new EmailNotification();
            case "SMS"   -> new SMSNotification();
            case "PUSH"  -> new PushNotification();
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}
```

#### Builder
```java
// Lombok @Builder tự generate — dùng phổ biến trong Spring
@Builder @Data
public class User {
    private Long id;
    private String name;
    private String email;
    private Role role;
}
// Dùng: User.builder().id(1L).name("John").role(Role.ADMIN).build();
```

### Structural

#### Decorator
```java
// Thêm behavior mà không sửa class gốc
// Java I/O là ví dụ điển hình: new BufferedReader(new FileReader(path))
interface CoffeeService { double getCost(); String getDesc(); }
class MilkDecorator implements CoffeeService {
    private final CoffeeService wrapped;
    MilkDecorator(CoffeeService c) { this.wrapped = c; }
    public double getCost() { return wrapped.getCost() + 0.5; }
    public String getDesc() { return wrapped.getDesc() + ", milk"; }
}
// new MilkDecorator(new SugarDecorator(new SimpleCoffee()))
```

#### Proxy
```java
// Spring AOP, @Transactional, @Cacheable đều dùng Proxy
interface UserService { User findById(Long id); }
class CachedUserProxy implements UserService {
    private final UserService real;
    private final Cache cache;
    public User findById(Long id) {
        return cache.getOrLoad("user:" + id, () -> real.findById(id));
    }
}
```

#### Adapter
```java
// Tích hợp legacy / third-party API
interface ModernGateway { PaymentResult charge(PaymentRequest req); }
class LegacyStripeAdapter implements ModernGateway {
    private final LegacyStripeClient legacy;
    public PaymentResult charge(PaymentRequest req) {
        boolean ok = legacy.processPayment(req.getCard(), req.getAmount());
        return new PaymentResult(ok);
    }
}
```

### Behavioral

#### Strategy
```java
// Đổi algorithm lúc runtime, tránh if/else phức tạp
interface PricingStrategy { double calculate(Order order); }
class RegularPricing    implements PricingStrategy { /* ... */ }
class VIPPricing        implements PricingStrategy { /* ... */ }
class BlackFridayPricing implements PricingStrategy { /* ... */ }

class OrderService {
    private PricingStrategy pricing;
    void setPricing(PricingStrategy p) { this.pricing = p; }
    double total(Order order) { return pricing.calculate(order); }
}
```

#### Observer (Spring Events)
```java
@Component
class OrderService {
    @Autowired ApplicationEventPublisher publisher;
    public void placeOrder(Order order) {
        orderRepo.save(order);
        publisher.publishEvent(new OrderPlacedEvent(this, order));
    }
}
@Component
class EmailHandler {
    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        emailService.sendConfirmation(event.getOrder());
    }
}
@Component
class InventoryHandler {
    @EventListener
    @Async // chạy async
    public void onOrderPlaced(OrderPlacedEvent event) {
        inventoryService.reserve(event.getOrder());
    }
}
```

#### Template Method
```java
// Define skeleton, subclass fill in details
abstract class DataImporter {
    public final void importData(String source) { // final — không override
        String raw     = readData(source);
        List<?> parsed = parseData(raw);   // abstract — phải override
        validateData(parsed);              // optional hook
        saveData(parsed);
    }
    protected abstract List<?> parseData(String raw);
    protected void validateData(List<?> data) { /* default: no-op */ }
}
class CSVImporter  extends DataImporter { /* ... */ }
class JSONImporter extends DataImporter { /* ... */ }
```

---

## 3. Microservices Architecture

### Microservices vs Monolith

|  | Monolith | Microservices |
|--|---------|---------------|
| Deploy | Đơn giản, 1 artifact | Phức tạp, nhiều service |
| Scale | Scale toàn bộ app | Scale từng service độc lập |
| Team | 1 codebase, dễ sync | Nhiều team autonomous |
| Latency | Thấp (in-process) | Cao hơn (network) |
| Database | Shared DB | DB riêng mỗi service |
| Debugging | Dễ | Cần distributed tracing |
| **Dùng khi** | Startup, team nhỏ | Scale lớn, nhiều domain rõ |

> Martin Fowler: **"Monolith First"** — bắt đầu với monolith, refactor thành microservices khi domain đã rõ ràng

### Key Patterns

#### API Gateway
```
Client → API Gateway → [Auth, RateLimit, Route] → Services
Tools: Kong, AWS API Gateway, Spring Cloud Gateway, Nginx
Xử lý: Authentication, SSL termination, Rate limiting, Load balancing
```

#### Circuit Breaker (Resilience4j)
```java
// CLOSED (bình thường) → OPEN (fail fast) → HALF_OPEN (thử lại)
@CircuitBreaker(name = "paymentSvc", fallbackMethod = "paymentFallback")
public PaymentResult charge(PaymentRequest req) {
    return paymentClient.charge(req); // có thể lỗi
}
public PaymentResult paymentFallback(PaymentRequest req, Exception e) {
    log.warn("Payment service down, queuing: {}", e.getMessage());
    return PaymentResult.queued(); // graceful degradation
}
```

#### Saga Pattern — Distributed Transaction
```
Choreography (Event-driven, loose coupling):
  OrderSvc → OrderCreated
  → PaymentSvc → PaymentDone
  → InventorySvc → StockReserved
  Rollback: PaymentFailed → compensating events

Orchestration (Central coordinator):
  SagaOrchestrator gọi từng service tuần tự
  Nếu lỗi: orchestrator gọi compensating transaction
```

#### Service Discovery
```
Eureka / Consul: service đăng ký khi start, deregister khi down
Client-side LB: Feign/Ribbon đọc registry → chọn instance
Server-side LB: ALB / nginx → client gọi 1 VIP
```

---

## 4. Database & Storage

### Index Strategy
```sql
-- Composite index: leading column quan trọng!
CREATE INDEX idx_status_date ON orders(status, created_at);
-- OK:  WHERE status = 'PENDING' AND created_at > ?
-- OK:  WHERE status = 'PENDING'
-- BAD: WHERE created_at > ?  (thiếu leading column)

-- Covering index: không cần scan table
CREATE INDEX idx_covering ON users(email) INCLUDE (name, status);

-- Partial index: chỉ index subset của data
CREATE INDEX idx_active ON users(email) WHERE deleted_at IS NULL;
```

### Transaction Isolation Levels

| Level | Dirty Read | Non-Repeatable | Phantom |
|-------|-----------|----------------|---------|
| READ_UNCOMMITTED | Có | Có | Có |
| READ_COMMITTED | Không | Có | Có |
| REPEATABLE_READ | Không | Không | Có |
| SERIALIZABLE | Không | Không | Không |

> MySQL default: **REPEATABLE_READ**  
> PostgreSQL default: **READ_COMMITTED**

### Caching Strategies

```
Cache-Aside (Lazy Loading) — phổ biến nhất:
  Read: App → cache miss → DB → populate cache
  Write: App → DB → invalidate cache key

Write-Through:
  Write: App → cache → DB đồng thời
  Đảm bảo cache luôn fresh, tốn latency write

Write-Behind (Write-Back):
  Write: App → cache → (async batch) → DB
  Write performance cao nhất, risk mất data nếu crash
```

---

## 5. System Design

### CAP Theorem
```
        Consistency
           /\
          /  \
         / CP \ (PostgreSQL, ZooKeeper)
        /      \
       /   CA   \  (không tồn tại trong distributed)
      /    (X)   \
     /____________\
Availability  --  Partition Tolerance
     AP: Cassandra, DynamoDB, CouchDB
```

### Rate Limiting Algorithms

| Algorithm | Đặc điểm | Dùng khi |
|-----------|----------|----------|
| Token Bucket | Burst OK, refill liên tục | API với burst traffic |
| Leaky Bucket | Process đều đặn, no burst | Smooth traffic |
| Fixed Window | Đơn giản, boundary issue | Simple use cases |
| Sliding Window | Chính xác, distributed | Production-grade |

### Message Queue

```
Kafka: high throughput, persistent, replay messages
  → Event streaming, audit log, analytics, event sourcing

RabbitMQ: flexible routing, low latency, AMQP
  → Task queue, RPC, complex routing rules

Dùng queue khi:
  - Decouple services (producer không cần biết consumer)
  - Async processing (email, notification, heavy processing)
  - Rate limiting / buffering spike traffic
  - Fan-out (1 event → nhiều consumer)
```

---

## 6. REST API Best Practices

```
Endpoints:
  GET    /api/v1/orders          list với pagination
  GET    /api/v1/orders/{id}     get one
  POST   /api/v1/orders          create → 201 Created
  PUT    /api/v1/orders/{id}     full update
  PATCH  /api/v1/orders/{id}     partial update
  DELETE /api/v1/orders/{id}     delete → 204 No Content

Pagination:
  GET /orders?page=0&size=20&sort=createdAt,desc
  Response: { content: [...], totalElements: 100, totalPages: 5 }

Standard Error Response:
  { "status": 400, "error": "Validation Failed",
    "message": "Email is invalid", "timestamp": "..." }

Idempotency cho POST:
  Idempotency-Key: <client-generated-uuid>
  Server lưu key → trả cùng result nếu duplicate
```

---

## 7. Security Basics

```java
// JWT Flow
// 1. Login → Server tạo JWT signed bằng secret/RSA key
// 2. Client lưu token (cookie httpOnly HOẶC memory, KHÔNG localStorage)
// 3. Request: Authorization: Bearer <access_token>
// 4. Server: verify signature → extract claims → authorize

// Access Token: short-lived (15min - 1h)
// Refresh Token: long-lived (7-30 days), lưu DB để có thể revoke

// Spring Security filter chain
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/public/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
    )
    .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

---

## 8. Quick Q&A

**Q: Khi nào chọn Microservices thay Monolith?**
> Team lớn (2 pizza rule), cần deploy độc lập, các domain có scaling requirements khác nhau. Bắt đầu Monolith → refactor khi thấy bounded context rõ ràng.

**Q: Eventual Consistency là gì?**
> Trong distributed system, data có thể tạm thời inconsistent, nhưng sẽ converge về đúng. Chấp nhận stale data ngắn để đổi lấy availability + performance (AP trong CAP).

**Q: CQRS + Event Sourcing?**
> CQRS: tách model đọc (optimized views) và ghi (domain model). Event Sourcing: lưu events thay state hiện tại → state = replay events. Kết hợp: write side phát events → read side project thành query-optimized views.
