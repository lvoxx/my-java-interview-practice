package io.github.lvoxx;

import java.util.ArrayDeque;

public class ArrayDequeDemo {
    public static void main(String[] args) {
        // ArrayDeque is a simple implementation of a queue using an array
        // It provides O(1) time complexity for enqueue and dequeue operations

        ArrayDeque<Integer> queue = new ArrayDeque<>(10);

        // Enqueue elements
        for (int i = 0; i < 10; i++) {
            queue.offer(i);
            System.out.println("Enqueued: " + i);
        }

        // Dequeue elements
        for (int i = 0; i < 10; i++) {
            int value = queue.poll();
            System.out.println("Dequeued: " + value);
        }

        System.out.println("Queue is empty: " + queue.isEmpty());
    }
}
