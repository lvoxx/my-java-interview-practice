# ☕ Java Core — Phỏng vấn Mid 3+ năm

---

## 1. JVM Internals

### Memory Model
```
┌─────────────────────────────────────────┐
│                   JVM                   │
│  ┌──────────┐  ┌──────────────────────┐ │
│  │  Stack   │  │        Heap          │ │
│  │per thread│  │  ┌────────┬───────┐  │ │
│  │- frames  │  │  │ Young  │  Old  │  │ │
│  │- locals  │  │  │(Eden,  │  Gen  │  │ │
│  │- refs    │  │  │ S0, S1)│       │  │ │
│  └──────────┘  │  └────────┴───────┘  │ │
│  ┌──────────┐  └──────────────────────┘ │
│  │Metaspace │  ┌──────────────────────┐ │
│  │(classes, │  │    Code Cache (JIT)  │ │
│  │ methods) │  └──────────────────────┘ │
│  └──────────┘                           │
└─────────────────────────────────────────┘
```

| Vùng nhớ | Chứa gì | GC? |
|----------|---------|-----|
| Heap Young Gen | Objects mới tạo | ✅ Minor GC |
| Heap Old Gen | Objects sống lâu | ✅ Major GC |
| Stack | Local variables, call frames | ❌ |
| Metaspace | Class metadata | ✅ khi class unload |

**Q: Stack Overflow vs OutOfMemoryError?**
- `StackOverflowError` — đệ quy quá sâu, stack frame hết chỗ
- `OutOfMemoryError` — heap hết, GC không giải phóng được

**Q: String pool nằm ở đâu?**
- Java 7+: String pool nằm trong **Heap** (trước đó là PermGen)

---

## 2. Garbage Collection

### GC Algorithms

| GC | Đặc điểm | Dùng khi |
|----|----------|----------|
| Serial GC | Single-thread, stop-the-world | App nhỏ, single CPU |
| Parallel GC | Multi-thread, stop-the-world | Throughput cao, batch job |
| G1 GC | Chia region, concurrent | Default Java 9+, balanced |
| ZGC / Shenandoah | Near-zero pause (<10ms) | Latency-sensitive, heap lớn |

```bash
# Chỉ định GC
java -XX:+UseG1GC -Xms512m -Xmx2g -jar app.jar
java -XX:+UseZGC -jar app.jar

# Heap dump khi OOM
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heap.hprof
```

---

## 3. Collections Framework

### Map toàn cảnh
```
Collection
├── List
│   ├── ArrayList        O(1) get, O(n) insert giữa, không thread-safe
│   ├── LinkedList       O(n) get, O(1) insert đầu/cuối
│   └── CopyOnWriteArrayList  thread-safe, tốt cho read-heavy
├── Set
│   ├── HashSet          O(1), không có thứ tự
│   ├── LinkedHashSet    giữ insertion order
│   └── TreeSet          sorted, O(log n)
└── Queue
    ├── ArrayDeque       faster than Stack/LinkedList
    └── PriorityQueue    min-heap

Map
├── HashMap              O(1) avg, null key OK, không thread-safe
├── LinkedHashMap        giữ insertion order
├── TreeMap              sorted by key, O(log n)
└── ConcurrentHashMap    thread-safe, dùng thay Hashtable
```

### HashMap Internal (hay hỏi!)
```java
// Java 8+: bucket là LinkedList → chuyển thành Tree khi >= 8 entries
// hash(key) → index = (n-1) & hash
// Default load factor = 0.75, capacity luôn là power-of-2
// Resize khi: size > capacity * loadFactor

// Tại sao phải override hashCode khi override equals?
// Contract: a.equals(b) == true → a.hashCode() == b.hashCode()
// Nếu không: HashMap sẽ không tìm thấy key dù equals trả về true!
```

---

## 4. Multithreading & Concurrency

### synchronized vs volatile vs Lock
```java
// volatile: đảm bảo VISIBILITY (không cache), KHÔNG đảm bảo atomicity
private volatile boolean running = true;

// synchronized: mutual exclusion + visibility
public synchronized void increment() { count++; } // lock trên this

// ReentrantLock: linh hoạt hơn, có tryLock, fairness
Lock lock = new ReentrantLock();
lock.lock();
try { /* critical section */ }
finally { lock.unlock(); } // LUÔN unlock trong finally!

// AtomicInteger: lock-free, dùng CAS (Compare-And-Swap)
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet(); // atomic, thread-safe
```

### ThreadPool — Best Practice
```java
// ĐỪNG dùng Executors.newCachedThreadPool() trong production
// → unlimited threads, có thể OOM

// Dùng ThreadPoolExecutor trực tiếp để kiểm soát
ExecutorService pool = new ThreadPoolExecutor(
    4,           // corePoolSize    — luôn duy trì 4 thread
    10,          // maximumPoolSize — max 10 thread khi queue đầy
    60L,         // keepAliveTime
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100), // bounded queue — tránh OOM!
    new ThreadPoolExecutor.CallerRunsPolicy() // rejection: caller tự chạy
);
```

### CompletableFuture
```java
CompletableFuture
    .supplyAsync(() -> fetchUser(id))          // async, ForkJoinPool
    .thenApply(user -> enrichUser(user))        // transform (map)
    .thenCompose(user -> fetchOrders(user.id))  // flatMap
    .thenAccept(orders -> process(orders))      // consume
    .exceptionally(ex -> { log(ex); return null; });

// Chạy song song, đợi tất cả
CompletableFuture.allOf(f1, f2, f3).join();
```

### Concurrency Issues

| Vấn đề | Nguyên nhân | Giải pháp |
|--------|------------|-----------|
| Race condition | Nhiều thread ghi cùng lúc | synchronized, Atomic |
| Deadlock | Thread A chờ B, B chờ A | Lock ordering, tryLock timeout |
| Starvation | Thread không bao giờ được CPU | Fair lock `new ReentrantLock(true)` |
| Memory visibility | Thread đọc stale data từ cache | volatile, synchronized |

---

## 5. Spring Framework

### IoC & Dependency Injection
```java
// Constructor Injection — RECOMMENDED (immutable, testable, no hidden deps)
@Service
public class OrderService {
    private final UserRepository userRepo;
    private final PaymentService paymentService;

    public OrderService(UserRepository userRepo, PaymentService paymentService) {
        this.userRepo = userRepo;
        this.paymentService = paymentService;
    }
}

// Field Injection với @Autowired — KHÔNG khuyến khích
// Lý do: không thể inject qua constructor trong test, khó phát hiện circular deps
```

### Bean Scope

| Scope | Mô tả |
|-------|-------|
| `singleton` | Default — 1 instance/ApplicationContext |
| `prototype` | Instance mới mỗi lần inject |
| `request` | 1 instance/HTTP request |
| `session` | 1 instance/HTTP session |

### AOP — Aspect Oriented Programming
```java
@Aspect @Component
public class LoggingAspect {
    @Around("@annotation(Loggable)")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        log.info("{} took {}ms", pjp.getSignature(), System.currentTimeMillis() - start);
        return result;
    }
}
// Dùng cho: logging, security check, transaction, caching, rate limiting
// AOP hoạt động qua proxy — chỉ intercept public method từ bên ngoài class!
```

### @Transactional — Pitfalls quan trọng
```java
@Transactional(
    propagation = Propagation.REQUIRED,     // default: join hoặc tạo mới
    isolation   = Isolation.READ_COMMITTED, // phổ biến nhất
    rollbackFor = Exception.class,          // rollback cả checked exception
    readOnly    = true                      // optimize cho SELECT
)

// ❌ TRAP: self-invocation bypass proxy
@Service class MyService {
    public void outer() {
        this.inner(); // gọi trực tiếp → @Transactional bị bỏ qua!
    }
    @Transactional
    public void inner() { ... }
}
// Fix: inject self bean hoặc tách ra service khác
```

**Transaction Propagation:**

| Propagation | Hành vi |
|------------|---------|
| `REQUIRED` | Join tx hiện tại, nếu không có thì tạo mới |
| `REQUIRES_NEW` | Luôn tạo tx mới, suspend tx cha |
| `NESTED` | Savepoint trong tx cha |
| `NOT_SUPPORTED` | Suspend tx hiện tại, chạy non-transactional |

---

## 6. Hibernate & JPA

### N+1 Problem
```java
// ❌ N+1: 1 query lấy 100 orders → 100 query lấy từng customer
List<Order> orders = orderRepo.findAll();
orders.forEach(o -> print(o.getCustomer().getName())); // LAZY → N queries!

// Fix 1: JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.customer")
List<Order> findAllWithCustomer();

// Fix 2: @EntityGraph
@EntityGraph(attributePaths = {"customer", "items"})
List<Order> findAll();

// Fix 3: @BatchSize
@BatchSize(size = 25) // load 25 customers/batch thay vì 1/1
@ManyToOne(fetch = FetchType.LAZY)
private Customer customer;
```

### Isolation Levels

| Level | Dirty Read | Non-Repeatable | Phantom |
|-------|-----------|----------------|---------|
| READ_UNCOMMITTED | ✅ có | ✅ có | ✅ có |
| READ_COMMITTED | ❌ | ✅ có | ✅ có |
| REPEATABLE_READ | ❌ | ❌ | ✅ có |
| SERIALIZABLE | ❌ | ❌ | ❌ |

---

## 7. Java 8–21 Modern Features

### Stream API
```java
// Collect với groupingBy
Map<String, Long> countByDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDept, Collectors.counting()));

// FlatMap
List<String> allSkills = employees.stream()
    .flatMap(e -> e.getSkills().stream())
    .distinct()
    .collect(Collectors.toList());
```

### Optional — dùng đúng cách
```java
// ❌ Sai — không khác null check
if (findUser(id).isPresent()) return findUser(id).get();

// ✅ Đúng
return findUser(id)
    .map(User::getName)
    .orElseThrow(() -> new UserNotFoundException(id));
```

### Virtual Threads (Java 21 — Project Loom)
```java
// Platform thread: 1 OS thread, ~1MB stack
// Virtual thread: lightweight, millions thread chạy đồng thời
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> handleRequest(req));
}
// Dùng cho: I/O-bound (HTTP calls, DB queries)
// KHÔNG dùng cho: CPU-bound (sẽ không có lợi)
```

---

## 8. Quick Fire Q&A

| Câu hỏi | Trả lời nhanh |
|---------|--------------|
| `==` vs `.equals()`? | `==` so sánh reference, `.equals()` so sánh giá trị |
| Tại sao String immutable? | String pool, security (hashCode cache), thread-safe |
| `final` vs `finally` vs `finalize`? | keyword / try-catch block / deprecated GC method |
| `HashMap` thread-safe? | Không, dùng `ConcurrentHashMap` |
| Checked vs Unchecked exception? | Checked = phải handle (IOException), Unchecked = RuntimeException |
| `interface` vs `abstract class`? | Interface: multiple impl, default methods; Abstract: state + partial impl |
| `Comparable` vs `Comparator`? | Comparable: natural order trong class; Comparator: external, linh hoạt |
| `Stack` vs `Deque`? | Dùng `ArrayDeque` thay `Stack`, nhanh hơn, không synchronized |
