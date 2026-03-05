package io.github.lvoxx.nplus1_query.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * Simple query counter để đếm số lượng SQL queries được thực thi
 */
@Component
public class QueryCounterService {

    private final AtomicInteger counter = new AtomicInteger(0);

    public void reset() {
        counter.set(0);
    }

    public void increment() {
        counter.incrementAndGet();
    }

    public int getCount() {
        return counter.get();
    }
}