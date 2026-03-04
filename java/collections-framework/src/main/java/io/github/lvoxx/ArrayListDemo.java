
package io.github.lvoxx;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the performance characteristics of ArrayList operations.
 * 
 * This class provides examples of common ArrayList operations and their
 * time complexities:
 * <ul>
 * <li><b>add(E element)</b> - O(1) average case: Appending to the end</li>
 * <li><b>get(int index)</b> - O(1): Accessing element by index</li>
 * <li><b>add(int index, E element)</b> - O(n): Inserting at a specific index
 * requires element shifting</li>
 * </ul>
 * 
 * <p>
 * The main method illustrates these operations with a String ArrayList,
 * showing how insertion at intermediate positions requires shifting subsequent
 * elements, resulting in linear time complexity.
 * </p>
 * 
 * @author lvoxx
 * @version 1.0
 */
public class ArrayListDemo {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();

        // Adding an element to the end of the list is O(1) on average
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100_000; i++) {
            list.add("Element " + i);
        }
        long end = System.currentTimeMillis();
        System.out.println("Time to add elements: " + (end - start) + " ms");

        list.get(1); // O(1) - Accessing an element by index is O(1)
        System.out.println(list.get(1)); // Output: B

        start = System.currentTimeMillis();
        for (int i = 0; i < 100_000; i++) {
            list.add(1, "Inserted " + i); // O(n) - Inserting at index 1 requires shifting
        }
        end = System.currentTimeMillis();
        System.out.println("Time to insert at index 1: " + (end - start) + " ms");
    }

}
