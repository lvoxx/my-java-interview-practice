package io.github.lvoxx.nplus1_query.dto;

import java.util.List;

public record QueryResult(
        String scenario,
        String description,
        int totalQueries,
        long executionTimeMs,
        List<AuthorDTO> authors,
        String sqlPattern,
        String recommendation) {
    public record AuthorDTO(Long id, String name, String email, List<String> bookTitles) {
    }
}