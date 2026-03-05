package io.github.lvoxx.nplus1_query.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.lvoxx.nplus1_query.model.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
}
