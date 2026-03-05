package io.github.lvoxx.nplus1_query.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lvoxx.nplus1_query.dto.QueryResult;
import io.github.lvoxx.nplus1_query.model.Author;
import io.github.lvoxx.nplus1_query.model.Book;
import io.github.lvoxx.nplus1_query.repository.AuthorRepository;
import io.github.lvoxx.nplus1_query.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class N1DemoService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    /**
     * ❌ PROBLEM: N+1 Query
     * - 1 query để lấy tất cả Authors
     * - N query để lấy Books của mỗi Author (N = số lượng authors)
     * Tổng: 1 + N queries
     */
    @Transactional(readOnly = true)
    public QueryResult demonstrateN1Problem() {
        log.warn("========== ❌ N+1 PROBLEM START ==========");
        long start = System.currentTimeMillis();

        // Query 1: SELECT * FROM authors → chỉ 1 query
        List<Author> authors = authorRepository.findAll();
        log.warn("Fetched {} authors with 1 query", authors.size());

        // Đây là vòng lặp GÂY RA N+1: mỗi lần gọi getBooks() → 1 query mới!
        List<QueryResult.AuthorDTO> dtos = authors.stream().map(author -> {
            // ⚠️ Trigger LAZY load → SELECT * FROM books WHERE author_id = ?
            List<String> bookTitles = author.getBooks().stream()
                    .map(Book::getTitle)
                    .toList();
            log.warn("  → Lazy loaded {} books for author: {}", bookTitles.size(), author.getName());
            return new QueryResult.AuthorDTO(author.getId(), author.getName(), author.getEmail(), bookTitles);
        }).toList();

        long elapsed = System.currentTimeMillis() - start;
        int totalQueries = 1 + authors.size(); // 1 for authors + N for each author's books

        log.warn("========== ❌ N+1 PROBLEM END: {} total queries ==========", totalQueries);

        return new QueryResult(
                "N+1 PROBLEM",
                "1 query lấy tất cả Authors + " + authors.size() + " queries riêng cho Books của mỗi Author",
                totalQueries,
                elapsed,
                dtos,
                "SELECT * FROM authors;\n" +
                        "-- Sau đó lặp N lần:\n" +
                        "SELECT * FROM books WHERE author_id = 1;\n" +
                        "SELECT * FROM books WHERE author_id = 2;\n" +
                        "SELECT * FROM books WHERE author_id = 3;\n" +
                        "-- ... (N queries nữa)",
                "❌ TRÁNH: Với 100 authors → 101 queries! Hiệu năng rất kém khi data lớn.");
    }

    /**
     * ✅ SOLUTION 1: JOIN FETCH
     * Chỉ 1 query duy nhất với JOIN để load cả Author và Books
     */
    @Transactional(readOnly = true)
    public QueryResult demonstrateSolution1JoinFetch() {
        log.info("========== ✅ SOLUTION 1: JOIN FETCH START ==========");
        long start = System.currentTimeMillis();

        // Chỉ 1 query: SELECT a.*, b.* FROM authors a JOIN books b ON a.id =
        // b.author_id
        List<Author> authors = authorRepository.findAllWithBooksJoinFetch();
        log.info("Fetched {} authors WITH books in 1 single query!", authors.size());

        List<QueryResult.AuthorDTO> dtos = authors.stream().map(author -> {
            // ✅ Không trigger thêm query nào - đã được load sẵn!
            List<String> bookTitles = author.getBooks().stream()
                    .map(Book::getTitle)
                    .toList();
            return new QueryResult.AuthorDTO(author.getId(), author.getName(), author.getEmail(), bookTitles);
        }).toList();

        long elapsed = System.currentTimeMillis() - start;
        log.info("========== ✅ SOLUTION 1 END: 1 total query ==========");

        return new QueryResult(
                "SOLUTION 1: JOIN FETCH",
                "1 query duy nhất load cả Authors và Books bằng JOIN FETCH",
                1,
                elapsed,
                dtos,
                "SELECT DISTINCT a.*, b.*\n" +
                        "FROM authors a\n" +
                        "JOIN books b ON a.id = b.author_id",
                "✅ TỐT: Chỉ 1 query, hiệu năng cao. Dùng khi luôn cần load books cùng authors.");
    }

    /**
     * ✅ SOLUTION 2: @EntityGraph
     * Dùng annotation để chỉ định eager loading cho specific queries
     */
    @Transactional(readOnly = true)
    public QueryResult demonstrateSolution2EntityGraph() {
        log.info("========== ✅ SOLUTION 2: LEFT JOIN FETCH START ==========");
        long start = System.currentTimeMillis();

        // LEFT JOIN để include cả authors không có books
        List<Author> authors = authorRepository.findAllWithBooksLeftJoinFetch();
        log.info("Fetched {} authors (including those without books) in 1 query", authors.size());

        List<QueryResult.AuthorDTO> dtos = authors.stream().map(author -> {
            List<String> bookTitles = author.getBooks().stream()
                    .map(Book::getTitle)
                    .toList();
            return new QueryResult.AuthorDTO(author.getId(), author.getName(), author.getEmail(), bookTitles);
        }).toList();

        long elapsed = System.currentTimeMillis() - start;
        log.info("========== ✅ SOLUTION 2 END: 1 total query ==========");

        return new QueryResult(
                "SOLUTION 2: LEFT JOIN FETCH",
                "1 query với LEFT JOIN FETCH - include cả Authors không có Books",
                1,
                elapsed,
                dtos,
                "SELECT a.*, b.*\n" +
                        "FROM authors a\n" +
                        "LEFT JOIN books b ON a.id = b.author_id",
                "✅ TỐT: Chỉ 1 query, giữ lại authors không có books. Linh hoạt hơn INNER JOIN.");
    }

    /**
     * So sánh hiệu năng giữa N+1 và các giải pháp
     */
    @Transactional(readOnly = true)
    public ComparisonResult compareAll() {
        log.info("========== PERFORMANCE COMPARISON ==========");

        QueryResult n1Result = demonstrateN1Problem();
        QueryResult sol1Result = demonstrateSolution1JoinFetch();
        QueryResult sol2Result = demonstrateSolution2EntityGraph();

        return new ComparisonResult(n1Result, sol1Result, sol2Result);
    }

    public record ComparisonResult(
            QueryResult n1Problem,
            QueryResult solution1,
            QueryResult solution2) {
    }
}
