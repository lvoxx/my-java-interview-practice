package io.github.lvoxx;

public class LinkedHashSetDemo {
    public static void main(String[] args) {
        // LinkedHashSet is a collection that does not allow duplicate elements and
        // maintains the insertion order.
        // It is implemented as a hash table with a linked list running through it.

        // Example usage of LinkedHashSet
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();

        long start = System.currentTimeMillis();
        // Adding elements to the LinkedHashSet
        for (int i = 0; i < 100_000; i++) {
            set.add("Element " + i);
        }
        long end = System.currentTimeMillis();
        System.out.println("Time to add elements: " + (end - start) + " ms");

        // Checking if an element exists in the LinkedHashSet
        System.out.println("Contains 'Element 100': " + set.contains("Element 100")); // Output: true
        System.out.println("Contains 'Element 200': " + set.contains("Element 200")); // Output: false

        // Check if the LinkedHashSet maintains insertion order
        for (String element : set) {
            System.out.println(element);
            if (element.equals("Element 5")) {
                break; // Print only the first 5 elements for demonstration
            }
        }
    }
}
