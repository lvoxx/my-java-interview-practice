package io.github.lvoxx;

import java.util.TreeSet;

public class TreeSetDemo {
    public static void main(String[] args) {
        // TreeSet is a NavigableSet implementation based on a Red-Black tree
        // It provides O(log n) time complexity for basic operations like add, remove,
        // and contains

        TreeSet<Integer> treeSet = new TreeSet<>();

        // Adding elements to the TreeSet
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100_000; i++) {
            treeSet.add(i);
        }
        long end = System.currentTimeMillis();
        System.out.println("Time to add elements: " + (end - start) + " ms");

        // Checking if an element exists in the TreeSet
        start = System.currentTimeMillis();
        boolean contains = treeSet.contains(50_000);
        end = System.currentTimeMillis();
        System.out.println("Contains 50,000: " + contains + ", Time taken: " + (end - start) + " ms");

        // Removing an element from the TreeSet
        start = System.currentTimeMillis();
        treeSet.remove(50_000);
        end = System.currentTimeMillis();
        System.out.println("Removed 50,000, Time taken: " + (end - start) + " ms");

        // Checking if the Set is sorted
        System.out.println("First element: " + treeSet.first()); // Should be 0
        System.out.println("Last element: " + treeSet.last()); // Should be 99_999

        TreeSet<Integer> subset = new TreeSet<>();
        subset.add(5);
        subset.add(1);
        subset.add(3);

        System.out.println("Subset (sorted): " + subset); // Should be [1, 3, 5]
    }
}
