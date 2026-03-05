package io.github.lvoxx.nplus1_query.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.lvoxx.nplus1_query.dto.QueryResult;
import io.github.lvoxx.nplus1_query.service.N1DemoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class N1DemoController {

    private final N1DemoService demoService;

    /**
     * Xem tất cả endpoints có sẵn
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> index() {
        return ResponseEntity.ok(Map.of(
                "❌ N+1 Problem", "GET /api/demo/n1-problem",
                "✅ Solution 1 (JOIN FETCH)", "GET /api/demo/solution-join-fetch",
                "✅ Solution 2 (LEFT JOIN)", "GET /api/demo/solution-left-join",
                "📊 Compare All", "GET /api/demo/compare",
                "🗄️ H2 Console", "http://localhost:8080/h2-console",
                "note", "Xem console/logs để thấy số lượng SQL queries!"));
    }

    /**
     * ❌ Demonstrate N+1 Problem
     * Quan sát console: thấy 1 + N queries
     */
    @GetMapping("/n1-problem")
    public ResponseEntity<QueryResult> n1Problem() {
        QueryResult result = demoService.demonstrateN1Problem();
        return ResponseEntity.ok(result);
    }

    /**
     * ✅ Solution 1: JOIN FETCH
     * Quan sát console: chỉ 1 query duy nhất
     */
    @GetMapping("/solution-join-fetch")
    public ResponseEntity<QueryResult> solutionJoinFetch() {
        QueryResult result = demoService.demonstrateSolution1JoinFetch();
        return ResponseEntity.ok(result);
    }

    /**
     * ✅ Solution 2: LEFT JOIN FETCH
     * Quan sát console: chỉ 1 query duy nhất
     */
    @GetMapping("/solution-left-join")
    public ResponseEntity<QueryResult> solutionLeftJoin() {
        QueryResult result = demoService.demonstrateSolution2EntityGraph();
        return ResponseEntity.ok(result);
    }

    /**
     * 📊 So sánh tất cả scenarios
     */
    @GetMapping("/compare")
    public ResponseEntity<N1DemoService.ComparisonResult> compareAll() {
        return ResponseEntity.ok(demoService.compareAll());
    }
}
