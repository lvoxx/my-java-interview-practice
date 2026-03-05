package io.github.lvoxx.nplus1_query.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.nplus1_query.model.Author;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    // ❌ BAD: Gây ra N+1 problem - chỉ fetch Author, books được load lazy sau đó
    // Khi gọi author.getBooks() → mỗi author tạo thêm 1 query riêng
    List<Author> findAll();

    // ✅ GOOD Solution 1: JOIN FETCH - load tất cả trong 1 query
    @Query("SELECT DISTINCT a FROM Author a JOIN FETCH a.books")
    List<Author> findAllWithBooksJoinFetch();

    // ✅ GOOD Solution 2: Entity Graph
    @Query("SELECT a FROM Author a LEFT JOIN FETCH a.books")
    List<Author> findAllWithBooksLeftJoinFetch();
}