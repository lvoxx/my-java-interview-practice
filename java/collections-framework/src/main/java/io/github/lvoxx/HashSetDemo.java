package io.github.lvoxx;

public class HashSetDemo {
    public static void main(String[] args) {
        // HashSet is a collection that does not allow duplicate elements and is based
        // on a hash table.
        // It provides constant time performance for basic operations like add, remove,
        // and contains.

        // Example usage of HashSet
        java.util.HashSet<String> set = new java.util.HashSet<>();

        // Adding elements to the HashSet
        set.add("Apple");
        set.add("Banana");
        set.add("Cherry");
        set.add("Apple"); // Duplicate element, will not be added

        System.out.println("HashSet contents: " + set); // Output: [Apple, Banana, Cherry]

        // Checking if an element exists in the HashSet
        System.out.println("Contains 'Banana': " + set.contains("Banana")); // Output: true
        System.out.println("Contains 'Grapes': " + set.contains("Grapes")); // Output: false

        // Removing an element from the HashSet
        set.remove("Banana");
        System.out.println("HashSet after removing 'Banana': " + set); // Output: [Apple, Cherry]
    }
}
