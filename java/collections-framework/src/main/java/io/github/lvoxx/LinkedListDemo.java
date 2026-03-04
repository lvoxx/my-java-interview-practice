package io.github.lvoxx;

import java.util.LinkedList;

/**
 * LinkedListDemo demonstrates the performance characteristics of Java's
 * LinkedList collection.
 * 
 * This class illustrates common operations on a LinkedList and their time
 * complexities:
 * <ul>
 * <li>add(E): O(1) - Adding elements to the end of the list is constant
 * time</li>
 * <li>get(int): O(n) - Accessing elements by index requires traversal from the
 * head</li>
 * </ul>
 * 
 * <p>
 * LinkedList is a doubly-linked list implementation that provides efficient
 * insertion
 * and deletion at both ends, but slower direct access by index compared to
 * ArrayList.
 * </p>
 * 
 * @author lvoxx
 * @version 1.0
 */
public class LinkedListDemo {
    public static void main(String[] args) {
        LinkedList<String> list = new LinkedList<>();

        list.add("A"); // O(1) - Adding to the end of the list
        list.add("B"); // O(1)
        list.add("C"); // O(1)

        list.get(1); // O(n) - Accessing an element by index requires traversal
        System.out.println(list.get(list.size() - 1)); // Output: C
    }
}
