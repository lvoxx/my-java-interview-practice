# 🔍 N+1 Query Problem Demo - Spring Boot + H2

Demo minh họa **N+1 Query Problem** trong JPA/Hibernate và các cách giải quyết.

---

## 🚀 Chạy ứng dụng

```bash
cd n1-query-demo
mvn spring-boot:run
```

Server khởi động tại: `http://localhost:8080`

---

## 📌 Vấn đề N+1 là gì?

```
N+1 = 1 query lấy danh sách (N items)
    + N queries riêng lẻ cho từng item
```

**Ví dụ với 5 Authors:**

```sql
-- Query 1: Lấy tất cả authors
SELECT * FROM authors;                          -- 1 query

-- Query 2-6: Lấy books của từng author (LAZY LOAD)
SELECT * FROM books WHERE author_id = 1;        -- query cho author 1
SELECT * FROM books WHERE author_id = 2;        -- query cho author 2
SELECT * FROM books WHERE author_id = 3;        -- query cho author 3
SELECT * FROM books WHERE author_id = 4;        -- query cho author 4
SELECT * FROM books WHERE author_id = 5;        -- query cho author 5

-- Tổng: 1 + 5 = 6 queries thay vì 1!
```

**Với 1000 authors → 1001 queries! 😱**

---

## 🌐 API Endpoints

| Method | URL                             | Mô tả                              |
| ------ | ------------------------------- | ---------------------------------- |
| `GET`  | `/api/demo`                     | Danh sách endpoints                |
| `GET`  | `/api/demo/n1-problem`          | ❌ Demo N+1 Problem (xem console!) |
| `GET`  | `/api/demo/solution-join-fetch` | ✅ Giải pháp: JOIN FETCH           |
| `GET`  | `/api/demo/solution-left-join`  | ✅ Giải pháp: LEFT JOIN FETCH      |
| `GET`  | `/api/demo/compare`             | 📊 So sánh tất cả                  |

### H2 Console

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: _(để trống)_

---

## 🔬 Cách quan sát N+1

1. Mở terminal và chạy app
2. Gọi `GET /api/demo/n1-problem`
3. Nhìn vào **console logs** - bạn sẽ thấy:

```
[WARN] ❌ N+1 PROBLEM START
[WARN] Fetched 5 authors with 1 query
Hibernate: select a1_0.id,a1_0.email,a1_0.name from authors a1_0  ← Query 1
[WARN]   → Lazy loaded 2 books for author: Joshua Bloch
Hibernate: select b1_0.author_id,b1_0.id,... from books b1_0 where b1_0.author_id=?  ← Query 2
[WARN]   → Lazy loaded 3 books for author: Martin Fowler
Hibernate: select b1_0.author_id,b1_0.id,... from books b1_0 where b1_0.author_id=?  ← Query 3
... (tiếp tục cho 4, 5)
[WARN] ❌ N+1 PROBLEM END: 6 total queries
```

4. Sau đó gọi `GET /api/demo/solution-join-fetch`
5. So sánh: chỉ **1 query** duy nhất!

---

## ✅ Giải pháp

### 1. JOIN FETCH (JPQL)

```java
@Query("SELECT DISTINCT a FROM Author a JOIN FETCH a.books")
List<Author> findAllWithBooksJoinFetch();
```

→ Tạo ra: `SELECT a.*, b.* FROM authors a INNER JOIN books b ON a.id = b.author_id`

### 2. LEFT JOIN FETCH

```java
@Query("SELECT a FROM Author a LEFT JOIN FETCH a.books")
List<Author> findAllWithBooksLeftJoinFetch();
```

→ Include cả authors không có books

### 3. @EntityGraph (annotation-based)

```java
@EntityGraph(attributePaths = {"books"})
List<Author> findAll();
```

### 4. Batch Size (cho nhiều collections)

```java
@BatchSize(size = 10)
@OneToMany(mappedBy = "author")
private List<Book> books;
```

→ Thay vì N queries, chỉ cần `N/10` queries (dùng IN clause)

---

## 📊 So sánh hiệu năng

| Scenario       | Queries | 5 Authors | 100 Authors | 1000 Authors |
| -------------- | ------- | --------- | ----------- | ------------ |
| ❌ N+1 Problem | 1 + N   | 6         | 101         | 1001         |
| ✅ JOIN FETCH  | 1       | 1         | 1           | 1            |
| ✅ LEFT JOIN   | 1       | 1         | 1           | 1            |

---

## 🏗️ Cấu trúc project

```
src/main/java/com/demo/n1/
├── N1QueryDemoApplication.java     # Main class
├── DataInitializer.java            # Seed data
├── entity/
│   ├── Author.java                 # @OneToMany (LAZY)
│   └── Book.java                   # @ManyToOne
├── repository/
│   ├── AuthorRepository.java       # Queries: findAll, JOIN FETCH, LEFT JOIN
│   └── BookRepository.java
├── service/
│   ├── N1DemoService.java          # Business logic + demo scenarios
│   └── QueryResult.java            # DTO
└── controller/
    └── N1DemoController.java       # REST endpoints
```
