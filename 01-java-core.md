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

| Vùng nhớ       | Chứa gì                      | GC?                 |
| -------------- | ---------------------------- | ------------------- |
| Heap Young Gen | Objects mới tạo              | ✅ Minor GC         |
| Heap Old Gen   | Objects sống lâu             | ✅ Major GC         |
| Stack          | Local variables, call frames | ❌                  |
| Metaspace      | Class metadata               | ✅ khi class unload |

**Q: Stack Overflow vs OutOfMemoryError?**

- `StackOverflowError` — đệ quy quá sâu, stack frame hết chỗ
- `OutOfMemoryError` — heap hết, GC không giải phóng được

**Q: String pool nằm ở đâu?**

- Java 7+: String pool nằm trong **Heap** (trước đó là PermGen)

---

## 2. Garbage Collection

### GC Algorithms

| GC               | Đặc điểm                      | Dùng khi                    |
| ---------------- | ----------------------------- | --------------------------- |
| Serial GC        | Single-thread, stop-the-world | App nhỏ, single CPU         |
| Parallel GC      | Multi-thread, stop-the-world  | Throughput cao, batch job   |
| G1 GC            | Chia region, concurrent       | Default Java 9+, balanced   |
| ZGC / Shenandoah | Near-zero pause (<10ms)       | Latency-sensitive, heap lớn |

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
└── Map
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

| Vấn đề            | Nguyên nhân                    | Giải pháp                           |
| ----------------- | ------------------------------ | ----------------------------------- |
| Race condition    | Nhiều thread ghi cùng lúc      | synchronized, Atomic                |
| Deadlock          | Thread A chờ B, B chờ A        | Lock ordering, tryLock timeout      |
| Starvation        | Thread không bao giờ được CPU  | Fair lock `new ReentrantLock(true)` |
| Memory visibility | Thread đọc stale data từ cache | volatile, synchronized              |

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

| Scope       | Mô tả                                   |
| ----------- | --------------------------------------- |
| `singleton` | Default — 1 instance/ApplicationContext |
| `prototype` | Instance mới mỗi lần inject             |
| `request`   | 1 instance/HTTP request                 |
| `session`   | 1 instance/HTTP session                 |

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

| Propagation     | Hành vi                                     |
| --------------- | ------------------------------------------- |
| `REQUIRED`      | Join tx hiện tại, nếu không có thì tạo mới  |
| `REQUIRES_NEW`  | Luôn tạo tx mới, suspend tx cha             |
| `NESTED`        | Savepoint trong tx cha                      |
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

| Level            | Dirty Read | Non-Repeatable | Phantom |
| ---------------- | ---------- | -------------- | ------- |
| READ_UNCOMMITTED | ✅ có      | ✅ có          | ✅ có   |
| READ_COMMITTED   | ❌         | ✅ có          | ✅ có   |
| REPEATABLE_READ  | ❌         | ❌             | ✅ có   |
| SERIALIZABLE     | ❌         | ❌             | ❌      |

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

| Câu hỏi                             | Trả lời nhanh                                                             |
| ----------------------------------- | ------------------------------------------------------------------------- |
| `==` vs `.equals()`?                | `==` so sánh reference, `.equals()` so sánh giá trị                       |
| Tại sao String immutable?           | String pool, security (hashCode cache), thread-safe                       |
| `final` vs `finally` vs `finalize`? | keyword / try-catch block / deprecated GC method                          |
| `HashMap` thread-safe?              | Không, dùng `ConcurrentHashMap`                                           |
| Checked vs Unchecked exception?     | Checked = phải handle (IOException), Unchecked = RuntimeException         |
| `interface` vs `abstract class`?    | Interface: multiple impl, default methods; Abstract: state + partial impl |
| `Comparable` vs `Comparator`?       | Comparable: natural order trong class; Comparator: external, linh hoạt    |
| `Stack` vs `Deque`?                 | Dùng `ArrayDeque` thay `Stack`, nhanh hơn, không synchronized             |

---

## 9. Câu hỏi phỏng vấn Java — Code Demo chi tiết

---

### Q1: Viết Singleton thread-safe, giải thích tại sao cần volatile

```java
// BAD: Not thread-safe
public class Singleton {
    private static Singleton instance;
    public static Singleton getInstance() {
        if (instance == null) {               // race condition!
            instance = new Singleton();
        }
        return instance;
    }
}

// GOOD: Double-checked locking + volatile
public class Singleton {
    // volatile ngăn CPU/JIT reorder instructions
    // new Singleton() thực ra gồm 3 bước:
    //   1. alloc memory
    //   2. init object
    //   3. assign reference
    // Không có volatile: bước 3 có thể xảy ra trước bước 2
    // → thread khác thấy instance != null nhưng object chưa init xong!
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {               // check 1: tránh lock không cần thiết
            synchronized (Singleton.class) {
                if (instance == null) {       // check 2: tránh tạo 2 lần
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// BEST: Enum Singleton — JVM đảm bảo thread-safe, serialization-safe
public enum Singleton {
    INSTANCE;
    public void doSomething() { ... }
}
// Dùng: Singleton.INSTANCE.doSomething()
```

---

### Q2: Deadlock — Demo + Cách phát hiện + Fix

```java
// Demo Deadlock
public class DeadlockDemo {
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (LOCK_A) {
                System.out.println("T1 holds A, waiting for B...");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (LOCK_B) {        // chờ T2 release B
                    System.out.println("T1 acquired B");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (LOCK_B) {
                System.out.println("T2 holds B, waiting for A...");
                synchronized (LOCK_A) {        // chờ T1 release A → DEADLOCK!
                    System.out.println("T2 acquired A");
                }
            }
        });

        t1.start();
        t2.start();
    }
}

// FIX 1: Lock ordering — luôn lấy lock theo thứ tự cố định
synchronized (LOCK_A) {      // cả 2 thread đều lấy A trước
    synchronized (LOCK_B) {  // rồi mới lấy B
        // ...
    }
}

// FIX 2: tryLock với timeout
Lock lockA = new ReentrantLock();
Lock lockB = new ReentrantLock();

boolean acquiredA = lockA.tryLock(1, TimeUnit.SECONDS);
boolean acquiredB = lockB.tryLock(1, TimeUnit.SECONDS);
if (acquiredA && acquiredB) {
    try { /* work */ }
    finally {
        lockA.unlock();
        lockB.unlock();
    }
} else {
    // giải phóng lock đã lấy được và retry sau
    if (acquiredA) lockA.unlock();
    if (acquiredB) lockB.unlock();
}

// Phát hiện Deadlock:
// jstack <pid>          → in thread dump, thấy "BLOCKED" + "waiting to lock"
// jconsole / jvisualvm  → Thread tab, Detect Deadlock button
```

---

### Q3: Producer-Consumer với BlockingQueue

```java
// BlockingQueue tự handle wait/notify — không cần synchronized thủ công
public class ProducerConsumerDemo {
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>(10);

    static class Producer implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 0; i < 20; i++) {
                    String item = "item-" + i;
                    queue.put(item);              // block nếu queue đầy
                    System.out.println("Produced: " + item);
                    Thread.sleep(100);
                }
                queue.put("POISON_PILL");         // signal consumer dừng
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    String item = queue.take();   // block nếu queue rỗng
                    if ("POISON_PILL".equals(item)) break;
                    System.out.println("Consumed: " + item);
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        new Thread(new Producer()).start();
        new Thread(new Consumer()).start();
    }
}
```

---

### Q4: volatile đủ cho counter không? Tại sao cần AtomicInteger?

```java
// Demo: volatile KHÔNG đủ cho compound operations
public class VolatileCounterBug {
    private volatile int count = 0;

    // count++ thực ra là 3 bước: READ → INCREMENT → WRITE
    // 2 thread cùng READ thấy count=5 → cùng ghi 6 → lost update!
    public void increment() { count++; }  // NOT atomic!

    // Fix: synchronized
    public synchronized void incrementSafe() { count++; }

    // Fix tốt hơn: AtomicInteger dùng CAS (hardware instruction)
    private AtomicInteger atomicCount = new AtomicInteger(0);
    public void incrementAtomic() { atomicCount.incrementAndGet(); }
}

// Demo CAS (Compare-And-Swap) behavior
AtomicInteger counter = new AtomicInteger(5);
// compareAndSet(expected, update): chỉ update nếu current == expected
boolean success = counter.compareAndSet(5, 6); // true, counter = 6
boolean fail    = counter.compareAndSet(5, 7); // false, counter vẫn = 6

// LongAdder: tốt hơn AtomicLong trong high-contention scenarios
// Chia thành cells, mỗi thread update cell riêng → ít contention
LongAdder adder = new LongAdder();
adder.increment();
long total = adder.sum();
```

---

### Q5: HashMap vs ConcurrentHashMap — Internal hoạt động thế nào?

```java
// HashMap internal (Java 8+)
// - Array of buckets, mỗi bucket là LinkedList → Tree (TREEIFY_THRESHOLD = 8)
// - hash(key) → bucketIndex = hash & (capacity - 1)
// - Resize: khi size > capacity * 0.75 → double capacity, rehash all entries

// Vấn đề HashMap trong multi-thread:
// 1. Lost update: 2 threads cùng put vào cùng bucket
// 2. Infinite loop: Java 7 resize tạo circular link (Java 8 đã fix)
// 3. Inconsistent read: đọc trong lúc resize

// ConcurrentHashMap Java 8:
// - Không dùng segment lock nữa (Java 7 style)
// - Dùng synchronized trên từng bucket (bin) + CAS
// - Read: không lock gì cả (volatile reads)
// - Write: lock 1 bucket → chỉ block thread write vào cùng bucket
// → throughput cao hơn Hashtable (lock toàn bộ map)

// Các method atomic của ConcurrentHashMap
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// putIfAbsent: chỉ put nếu key chưa có — atomic!
map.putIfAbsent("key", 1);

// computeIfAbsent: tính value nếu key chưa có — atomic!
map.computeIfAbsent("userId", k -> loadFromDB(k));

// merge: update value một cách atomic
map.merge("count", 1, Integer::sum);  // tăng counter

// compute: read-modify-write atomic
map.compute("key", (k, v) -> v == null ? 1 : v + 1);

// Lưu ý: getOrDefault KHÔNG atomic với putIfAbsent
// Dùng computeIfAbsent thay vì if (map.get(k)==null) map.put(k, v)
```

---

### Q6: String Pool — Demo behavior

```java
public class StringPoolDemo {
    public static void main(String[] args) {
        // Literal → vào String Pool
        String s1 = "hello";
        String s2 = "hello";
        System.out.println(s1 == s2);          // true — cùng object trong pool

        // new String() → tạo object mới trong Heap
        String s3 = new String("hello");
        System.out.println(s1 == s3);          // false — khác object
        System.out.println(s1.equals(s3));     // true — cùng value

        // intern() → lấy reference từ pool (hoặc thêm vào pool)
        String s4 = s3.intern();
        System.out.println(s1 == s4);          // true

        // String concatenation behavior
        String a = "foo";
        String b = "bar";
        String c = a + b;                      // compile time: new StringBuilder().append(a).append(b).toString()
        String d = "foo" + "bar";              // compile time constant → "foobar" literal → pool
        System.out.println(c == d);            // false! c là object mới
        System.out.println(c.equals(d));       // true

        // Tại sao String immutable?
        // 1. String Pool: multiple references cùng trỏ vào 1 object
        //    → nếu mutable, 1 thay đổi ảnh hưởng tất cả
        // 2. HashCode được cache → dùng làm HashMap key hiệu quả
        // 3. Thread-safe mặc định: chia sẻ giữa threads không cần sync
        // 4. Security: class name, file path không thể bị thay đổi
    }
}
```

---

### Q7: Exception Handling — Best Practices

```java
// Custom Exception hierarchy
public class AppException extends RuntimeException {  // Unchecked
    private final ErrorCode code;

    public AppException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public AppException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);  // LUÔN chain cause để không mất stack trace
        this.code = code;
    }
}

public class UserNotFoundException extends AppException {
    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND,
              "User not found with id: " + userId);
    }
}

// BAD practices
try {
    doSomething();
} catch (Exception e) {
    // 1. Swallow exception — tệ nhất
}

try {
    doSomething();
} catch (Exception e) {
    throw new RuntimeException(e.getMessage());  // 2. Mất stack trace gốc!
}

// GOOD practices
try {
    doSomething();
} catch (IOException e) {
    log.error("Failed to process file: {}", filename, e);  // log với cause
    throw new FileProcessingException("Cannot process " + filename, e);  // chain cause
} finally {
    // cleanup — luôn chạy dù có exception hay không
    closeResource();
}

// Try-with-resources (Java 7+) — tự động đóng AutoCloseable
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    // dùng conn và ps
    // tự động gọi close() theo thứ tự ngược lại khi thoát block
}

// Multi-catch (Java 7+)
try {
    riskyOperation();
} catch (IOException | SQLException e) {
    log.error("Operation failed", e);
    throw new ServiceException("Service unavailable", e);
}
```

---

### Q8: Immutability — Cách tạo Immutable class

```java
// Immutable class: một khi đã tạo, không thể thay đổi state
public final class Money {                    // final: không cho extend
    private final BigDecimal amount;          // final: không reassign
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        // validate trong constructor
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Amount must be non-negative");
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency, "Currency required");
    }

    // Không có setter
    // Trả về object mới thay vì modify
    public Money add(Money other) {
        if (!this.currency.equals(other.currency))
            throw new IllegalArgumentException("Currency mismatch");
        return new Money(this.amount.add(other.amount), this.currency);
    }

    // Getter trả về primitive hoặc immutable object
    public BigDecimal getAmount() { return amount; }  // BigDecimal là immutable
    public Currency getCurrency() { return currency; }

    // Override equals/hashCode (vì là value object)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money m)) return false;
        return amount.compareTo(m.amount) == 0 && currency.equals(m.currency);
    }

    @Override
    public int hashCode() { return Objects.hash(amount, currency); }
}

// Java 16+: Record — immutable by default
public record Money(BigDecimal amount, Currency currency) {
    // Compact constructor: validate
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Negative amount");
    }
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

---

### Q9: Generics — Bounded Type, Wildcard, PECS

```java
// Bounded type parameter
public <T extends Comparable<T>> T max(List<T> list) {
    return list.stream().max(Comparator.naturalOrder())
               .orElseThrow(NoSuchElementException::new);
}

// Wildcard
List<Integer> ints    = List.of(1, 2, 3);
List<? extends Number> nums = ints;      // covariant — read only
// nums.add(4);                          // COMPILE ERROR! không biết exact type

List<? super Integer> superInts = new ArrayList<Number>(); // contravariant
superInts.add(42);                       // OK — có thể add Integer
// Integer i = superInts.get(0);        // COMPILE ERROR! chỉ biết là Object

// PECS: Producer Extends, Consumer Super
public <T> void copy(List<? extends T> src,   // producer: đọc từ src
                     List<? super T> dest) {   // consumer: ghi vào dest
    dest.addAll(src);
}

// Generic method vs wildcard
// Dùng generic method khi cần type parameter ở nhiều chỗ
public <T> List<T> repeat(T item, int times) { ... }
// Dùng wildcard khi chỉ cần linh hoạt trong 1 chỗ
public void printAll(List<?> list) { list.forEach(System.out::println); }

// Type erasure: generics bị xóa lúc runtime
List<String> strings = new ArrayList<>();
List<Integer> ints2  = new ArrayList<>();
System.out.println(strings.getClass() == ints2.getClass()); // TRUE! both are ArrayList
// → không thể: new T(), instanceof List<String>, T[].class
```

---

### Q10: Stream — Các trường hợp hay dùng + Performance

```java
// Reduce: tổng hợp
int sum = IntStream.rangeClosed(1, 100).sum();  // 5050
Optional<Integer> product = Stream.of(1,2,3,4,5).reduce((a, b) -> a * b);

// Collectors phức tạp
// groupingBy + downstream collector
Map<Department, DoubleSummaryStatistics> salaryStats = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.summarizingDouble(Employee::getSalary)
    ));

// Partition: chia thành 2 nhóm (true/false)
Map<Boolean, List<Integer>> evenOdd = IntStream.rangeClosed(1, 10).boxed()
    .collect(Collectors.partitioningBy(n -> n % 2 == 0));

// toMap với merge function (xử lý duplicate key)
Map<String, Employee> byName = employees.stream()
    .collect(Collectors.toMap(
        Employee::getName,
        e -> e,
        (existing, replacement) -> existing  // giữ existing nếu trùng key
    ));

// teeing (Java 12+): apply 2 downstream collectors
var result = Stream.of(1, 2, 3, 4, 5)
    .collect(Collectors.teeing(
        Collectors.summingInt(Integer::intValue),
        Collectors.counting(),
        (sum2, count) -> (double) sum2 / count  // average = 3.0
    ));

// Stream performance tips:
// 1. Lazy evaluation: intermediate ops không chạy cho đến terminal op
// 2. Short-circuit: findFirst, anyMatch dừng sớm
// 3. parallelStream: chỉ dùng khi: data lớn, CPU-bound, không có shared state
// 4. Tránh boxed() không cần thiết: dùng IntStream, LongStream, DoubleStream
IntStream.range(0, 1_000_000).sum();          // nhanh hơn
Stream.iterate(0, i -> i+1).limit(1_000_000) // chậm hơn (boxing)
    .mapToInt(Integer::intValue).sum();
```

---

### Q11: Functional Interface — Lambda, Method Reference

```java
// Built-in Functional Interfaces
Predicate<String>   nonEmpty  = s -> !s.isEmpty();          // boolean test(T t)
Function<String, Integer> len = String::length;             // R apply(T t)
Consumer<String>    printer   = System.out::println;        // void accept(T t)
Supplier<List<String>> listOf = ArrayList::new;             // T get()
BiFunction<Integer, Integer, Integer> add = Integer::sum;   // R apply(T, U)

// Function composition
Function<Integer, Integer> times2 = x -> x * 2;
Function<Integer, Integer> plus3  = x -> x + 3;
Function<Integer, Integer> times2ThenPlus3 = times2.andThen(plus3);
Function<Integer, Integer> plus3ThenTimes2 = times2.compose(plus3);
System.out.println(times2ThenPlus3.apply(4)); // (4*2)+3 = 11
System.out.println(plus3ThenTimes2.apply(4)); // (4+3)*2 = 14

// Method reference types
class Demo {
    static void staticMethod(String s) {}
    void instanceMethod(String s) {}
    String transform(String s) { return s.toUpperCase(); }
}
Demo obj = new Demo();
Consumer<String> c1 = Demo::staticMethod;     // static method ref
Consumer<String> c2 = obj::instanceMethod;    // instance method ref (bound)
Consumer<String> c3 = Demo::instanceMethod;   // instance method ref (unbound) — lấy target là tham số đầu

// Custom Functional Interface
@FunctionalInterface
interface Transformer<I, O> {
    O transform(I input);
    // Chỉ 1 abstract method — default method không tính
    default Transformer<I, O> andLog() {
        return input -> {
            O output = this.transform(input);
            System.out.println(input + " -> " + output);
            return output;
        };
    }
}
```

---

### Q12: Spring Boot — Request lifecycle + Filter vs Interceptor vs AOP

```java
// Request lifecycle
// Client Request
//   → DispatcherServlet
//   → HandlerMapping (tìm controller)
//   → HandlerInterceptor.preHandle()
//   → Controller method
//   → HandlerInterceptor.postHandle()
//   → ViewResolver / MessageConverter
//   → HandlerInterceptor.afterCompletion()
//   → Response

// Filter (Servlet level) — trước khi đến Spring
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
                                    throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        res.addHeader("X-Trace-Id", traceId);
        try {
            chain.doFilter(req, res);   // gọi filter tiếp theo / servlet
        } finally {
            log.info("{} {} {}ms", req.getMethod(), req.getRequestURI(),
                     System.currentTimeMillis() - start);
            MDC.clear();
        }
    }
}

// Interceptor (Spring MVC level) — biết về Handler/Controller
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req,
                              HttpServletResponse res,
                              Object handler) {
        String token = req.getHeader("Authorization");
        if (!jwtService.isValid(token)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;   // dừng lại, không gọi controller
        }
        return true;
    }
}

// AOP (Method level) — cross-cutting concern, không biết về HTTP
@Around("@annotation(RateLimit)")
public Object rateLimit(ProceedingJoinPoint pjp) throws Throwable {
    String key = "ratelimit:" + getClientIp();
    if (redisRateLimiter.isExceeded(key)) {
        throw new TooManyRequestsException();
    }
    return pjp.proceed();
}

// Thứ tự: Filter → Interceptor → AOP → Method
// Dùng Filter: auth header, CORS, compression, encoding
// Dùng Interceptor: logging, access control (biết về controller)
// Dùng AOP: business cross-cutting (audit, cache, retry, transaction)
```

---

### Q13: Lazy Initialization — ThreadLocal, ScopedValue

```java
// ThreadLocal: mỗi thread có bản sao riêng của variable
public class SecurityContextHolder {
    private static final ThreadLocal<UserContext> context =
        ThreadLocal.withInitial(UserContext::anonymous);

    public static UserContext get() { return context.get(); }
    public static void set(UserContext ctx) { context.set(ctx); }

    // QUAN TRỌNG: phải clear sau khi dùng
    // Thread pool tái sử dụng thread → ThreadLocal của request trước còn đó!
    public static void clear() { context.remove(); }
}

// Trong Filter:
try {
    SecurityContextHolder.set(parseUserFromToken(req));
    chain.doFilter(req, res);
} finally {
    SecurityContextHolder.clear();  // LUÔN clear trong finally!
}

// InheritableThreadLocal: child thread kế thừa từ parent
InheritableThreadLocal<String> traceId = new InheritableThreadLocal<>();
traceId.set("abc123");
new Thread(() -> System.out.println(traceId.get())).start();  // "abc123"

// ScopedValue (Java 21 Preview — thay thế ThreadLocal trong virtual threads)
static final ScopedValue<UserContext> USER = ScopedValue.newInstance();
ScopedValue.where(USER, currentUser).run(() -> {
    processRequest();  // USER.get() trả về currentUser trong scope này
});
```

---

### Q14: Memory Leak — Các nguyên nhân phổ biến

```java
// 1. Static collection tích lũy objects
public class LeakyCache {
    // static Map sống suốt lifetime của app!
    private static final Map<String, Object> cache = new HashMap<>();

    public void cache(String key, Object value) {
        cache.put(key, value);  // thêm mãi, không bao giờ xóa → leak!
    }
    // Fix: dùng WeakHashMap hoặc Caffeine/Guava cache với eviction policy
}

// 2. ThreadLocal không clear (trong thread pool)
// Xem Q13 — luôn clear trong finally!

// 3. Inner class giữ reference tới outer class
public class Outer {
    private byte[] largeData = new byte[1024 * 1024]; // 1MB

    class Inner { }   // non-static inner class giữ reference tới Outer!

    public Runnable createTask() {
        return new Inner();  // Outer không thể GC khi Runnable còn alive!
        // Fix: dùng static inner class
    }
}

// 4. Listener/Callback không deregister
eventBus.register(this);
// ...
eventBus.unregister(this);  // NHỚ unregister khi không dùng nữa!

// 5. InputStream không đóng
// Fix: dùng try-with-resources

// Cách phát hiện Memory Leak:
// jmap -heap <pid>                → xem heap usage
// jmap -histo <pid> | head -20    → top 20 class by instance count
// jcmd <pid> GC.run               → force GC rồi check lại
// heap dump + MAT (Memory Analyzer Tool)
// -XX:+HeapDumpOnOutOfMemoryError → tự dump khi OOM
```

---

### Q15: Java 8 Date/Time API

```java
// java.time (Java 8+) — immutable, thread-safe
// LocalDate / LocalTime / LocalDateTime — không có timezone
LocalDate today = LocalDate.now();
LocalDate birthday = LocalDate.of(1995, Month.JUNE, 15);
long age = ChronoUnit.YEARS.between(birthday, today);

// ZonedDateTime — có timezone
ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
ZonedDateTime utcNow = now.withZoneSameInstant(ZoneId.of("UTC"));

// Instant — machine time, epoch milliseconds
Instant start = Instant.now();
// ... do work ...
long elapsed = Duration.between(start, Instant.now()).toMillis();

// Period vs Duration
Period period = Period.between(LocalDate.of(2020, 1, 1), today);
System.out.println(period.getYears() + " years");  // date-based

Duration duration = Duration.ofHours(8).plusMinutes(30);
System.out.println(duration.toMinutes() + " minutes");  // time-based

// DateTimeFormatter
DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
String formatted = LocalDateTime.now().format(fmt);
LocalDateTime parsed = LocalDateTime.parse("25/12/2024 10:30:00", fmt);

// Với JPA: dùng Instant hoặc OffsetDateTime cho DB timestamp
// @CreatedDate / @LastModifiedDate với Spring Data Auditing
```

---

### Q16: Design một Generic Repository pattern

```java
// Generic Repository
public interface Repository<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T save(T entity);
    void deleteById(ID id);
    boolean existsById(ID id);
}

// Generic base implementation
public abstract class BaseJpaRepository<T, ID> implements Repository<T, ID> {
    @PersistenceContext
    protected EntityManager em;

    private final Class<T> entityClass;

    @SuppressWarnings("unchecked")
    protected BaseJpaRepository() {
        // Lấy actual type argument lúc runtime qua reflection
        this.entityClass = (Class<T>)
            ((ParameterizedType) getClass().getGenericSuperclass())
            .getActualTypeArguments()[0];
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(em.find(entityClass, id));
    }

    @Override
    public List<T> findAll() {
        return em.createQuery("FROM " + entityClass.getSimpleName(), entityClass)
                 .getResultList();
    }

    @Override
    public T save(T entity) {
        em.persist(entity);
        return entity;
    }
}

// Concrete repository
@Repository
@Transactional
public class UserRepositoryImpl extends BaseJpaRepository<User, Long> {
    // tự động có findById, findAll, save, deleteById

    // Thêm query đặc thù
    public Optional<User> findByEmail(String email) {
        return em.createQuery(
                "SELECT u FROM User u WHERE u.email = :email", User.class)
            .setParameter("email", email)
            .getResultStream()
            .findFirst();
    }
}
```

---

### Q17: Viết code kiểm tra 2 chuỗi có phải anagram không

```java
// Anagram: "listen" và "silent" — cùng ký tự, khác thứ tự
public class AnagramChecker {

    // Approach 1: Sort (O(n log n))
    public boolean isAnagramBySort(String s1, String s2) {
        if (s1.length() != s2.length()) return false;
        char[] a = s1.toLowerCase().toCharArray();
        char[] b = s2.toLowerCase().toCharArray();
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }

    // Approach 2: Frequency map (O(n)) — tốt hơn
    public boolean isAnagram(String s1, String s2) {
        if (s1.length() != s2.length()) return false;
        int[] freq = new int[26];              // chỉ lowercase a-z
        for (char c : s1.toLowerCase().toCharArray()) freq[c - 'a']++;
        for (char c : s2.toLowerCase().toCharArray()) freq[c - 'a']--;
        for (int count : freq) {
            if (count != 0) return false;
        }
        return true;
    }

    // Unicode-safe (dùng HashMap cho chars ngoài ASCII)
    public boolean isAnagramUnicode(String s1, String s2) {
        if (s1.length() != s2.length()) return false;
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : s1.toCharArray())
            freq.merge(c, 1, Integer::sum);
        for (char c : s2.toCharArray()) {
            freq.merge(c, -1, Integer::sum);
            if (freq.get(c) < 0) return false;  // early exit
        }
        return true;
    }
}
```

---

### Q18: Design một Rate Limiter đơn giản

```java
// Token Bucket Rate Limiter (in-memory, single instance)
public class TokenBucketRateLimiter {
    private final long capacity;          // max tokens
    private final double refillRatePerMs; // tokens per millisecond
    private double tokens;
    private long lastRefillTime;

    public TokenBucketRateLimiter(long capacity, long refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerMs = (double) refillRatePerSecond / 1000;
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    public synchronized boolean tryAcquire() {
        refill();
        if (tokens >= 1) {
            tokens--;
            return true;  // allowed
        }
        return false;     // rate limited
    }

    private void refill() {
        long now = System.currentTimeMillis();
        double tokensToAdd = (now - lastRefillTime) * refillRatePerMs;
        tokens = Math.min(capacity, tokens + tokensToAdd);
        lastRefillTime = now;
    }
}

// Redis-based Distributed Rate Limiter (Sliding Window)
@Component
public class RedisRateLimiter {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // Sliding window log: lưu timestamps của requests
    public boolean isAllowed(String userId, int maxRequests, Duration window) {
        String key = "ratelimit:" + userId;
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        // Lua script đảm bảo atomic
        String luaScript = """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            local count = redis.call('ZCARD', KEYS[1])
            if count < tonumber(ARGV[2]) then
                redis.call('ZADD', KEYS[1], ARGV[3], ARGV[3])
                redis.call('EXPIRE', KEYS[1], ARGV[4])
                return 1
            end
            return 0
            """;

        Long result = redisTemplate.execute(
            RedisScript.of(luaScript, Long.class),
            List.of(key),
            String.valueOf(windowStart),
            String.valueOf(maxRequests),
            String.valueOf(now),
            String.valueOf(window.toSeconds())
        );
        return Long.valueOf(1).equals(result);
    }
}
```

---

### Q19: CompletableFuture — Xử lý song song nhiều API calls

```java
@Service
public class DashboardService {

    // Gọi 3 service song song, tổng hợp kết quả
    public DashboardData getDashboard(Long userId) {
        CompletableFuture<UserProfile> profileFuture =
            CompletableFuture.supplyAsync(() -> userService.getProfile(userId));

        CompletableFuture<List<Order>> ordersFuture =
            CompletableFuture.supplyAsync(() -> orderService.getRecentOrders(userId));

        CompletableFuture<WalletBalance> walletFuture =
            CompletableFuture.supplyAsync(() -> walletService.getBalance(userId));

        // Chờ cả 3 xong rồi tổng hợp
        return CompletableFuture.allOf(profileFuture, ordersFuture, walletFuture)
            .thenApply(ignored -> DashboardData.builder()
                .profile(profileFuture.join())   // join() không throw checked
                .orders(ordersFuture.join())
                .wallet(walletFuture.join())
                .build())
            .exceptionally(ex -> {
                log.error("Dashboard load failed for user {}", userId, ex);
                return DashboardData.empty(userId);   // fallback
            })
            .join();  // block và lấy kết quả (trong controller nên dùng WebFlux hoặc async)
    }

    // Với timeout
    public UserProfile getProfileWithTimeout(Long userId) {
        return CompletableFuture
            .supplyAsync(() -> userService.getProfile(userId))
            .orTimeout(2, TimeUnit.SECONDS)  // Java 9+
            .exceptionally(ex -> {
                if (ex instanceof TimeoutException) {
                    return UserProfile.defaultProfile(userId);
                }
                throw new CompletionException(ex);
            })
            .join();
    }
}
```

---

### Q20: Viết LRU Cache không dùng thư viện

```java
// LRU Cache = HashMap + Doubly LinkedList
// HashMap: O(1) lookup
// LinkedList: O(1) move to front (most recently used)
public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> map = new HashMap<>();

    // Doubly linked list
    private final Node<K, V> head = new Node<>(null, null); // dummy head
    private final Node<K, V> tail = new Node<>(null, null); // dummy tail

    public LRUCache(int capacity) {
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) return null;
        moveToFront(node);   // recently used → front
        return node.value;
    }

    public void put(K key, V value) {
        Node<K, V> node = map.get(key);
        if (node != null) {
            node.value = value;
            moveToFront(node);
        } else {
            Node<K, V> newNode = new Node<>(key, value);
            map.put(key, newNode);
            addToFront(newNode);
            if (map.size() > capacity) {
                Node<K, V> lru = tail.prev;  // least recently used → back
                removeNode(lru);
                map.remove(lru.key);
            }
        }
    }

    private void moveToFront(Node<K, V> node) { removeNode(node); addToFront(node); }
    private void addToFront(Node<K, V> node) {
        node.prev = head; node.next = head.next;
        head.next.prev = node; head.next = node;
    }
    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next; node.next.prev = node.prev;
    }

    private static class Node<K, V> {
        K key; V value;
        Node<K, V> prev, next;
        Node(K key, V value) { this.key = key; this.value = value; }
    }
}

// Test
LRUCache<Integer, String> cache = new LRUCache<>(3);
cache.put(1, "one");
cache.put(2, "two");
cache.put(3, "three");
cache.get(1);           // access 1 → order: 3,2,1
cache.put(4, "four");   // evict 2 (LRU) → order: 4,3,1
System.out.println(cache.get(2)); // null (evicted)
```
