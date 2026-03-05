package io.github.lvoxx.nplus1_query;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.github.lvoxx.nplus1_query.model.Author;
import io.github.lvoxx.nplus1_query.model.Book;
import io.github.lvoxx.nplus1_query.repository.AuthorRepository;
import io.github.lvoxx.nplus1_query.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    @Override
    public void run(String... args) {
        log.info("🌱 Seeding demo data...");

        // Tạo Authors
        Author josh = authorRepository.save(Author.builder()
                .name("Joshua Bloch")
                .email("jbloch@example.com")
                .build());

        Author martin = authorRepository.save(Author.builder()
                .name("Martin Fowler")
                .email("mfowler@example.com")
                .build());

        Author robert = authorRepository.save(Author.builder()
                .name("Robert C. Martin")
                .email("uncle.bob@example.com")
                .build());

        Author vlad = authorRepository.save(Author.builder()
                .name("Vlad Mihalcea")
                .email("vlad@example.com")
                .build());

        Author eric = authorRepository.save(Author.builder()
                .name("Eric Evans")
                .email("eevans@example.com")
                .build());

        // Tạo Books cho từng Author
        bookRepository.saveAll(List.of(
                Book.builder().title("Effective Java").isbn("978-0134685991").price(45.99).author(josh).build(),
                Book.builder().title("Java Puzzlers").isbn("978-0321336781").price(39.99).author(josh).build()));

        bookRepository.saveAll(List.of(
                Book.builder().title("Refactoring").isbn("978-0134757599").price(49.99).author(martin).build(),
                Book.builder().title("Patterns of Enterprise Application Architecture").isbn("978-0321127426")
                        .price(54.99).author(martin).build(),
                Book.builder().title("UML Distilled").isbn("978-0321193681").price(34.99).author(martin).build()));

        bookRepository.saveAll(List.of(
                Book.builder().title("Clean Code").isbn("978-0132350884").price(42.99).author(robert).build(),
                Book.builder().title("Clean Architecture").isbn("978-0134494166").price(44.99).author(robert).build(),
                Book.builder().title("The Clean Coder").isbn("978-0137081073").price(38.99).author(robert).build()));

        bookRepository.saveAll(List.of(
                Book.builder().title("High-Performance Java Persistence").isbn("978-9730228236").price(59.99)
                        .author(vlad).build(),
                Book.builder().title("Java Persistence with Hibernate").isbn("978-1617290459").price(55.99).author(vlad)
                        .build()));

        bookRepository.saveAll(List.of(
                Book.builder().title("Domain-Driven Design").isbn("978-0321125217").price(52.99).author(eric).build()));

        long authorCount = authorRepository.count();
        long bookCount = bookRepository.count();

        log.info("✅ Seeded {} authors và {} books", authorCount, bookCount);
        log.info("🚀 Server started! Try these endpoints:");
        log.info("   ❌ N+1 Problem:    GET http://localhost:8080/api/demo/n1-problem");
        log.info("   ✅ Solution 1:     GET http://localhost:8080/api/demo/solution-join-fetch");
        log.info("   ✅ Solution 2:     GET http://localhost:8080/api/demo/solution-left-join");
        log.info("   📊 Compare All:    GET http://localhost:8080/api/demo/compare");
        log.info("   🗄️ H2 Console:     http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:testdb)");
    }
}
