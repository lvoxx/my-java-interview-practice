package io.github.lvoxx;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class CopyOnWriteDemo {
    public static void main(String[] args) {

        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>();

        int size = 100_000;

        // Measure time taken to add elements to the list
        long start = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        long end = System.currentTimeMillis();

        System.out.println("Time to write: " + (end - start) + " ms");

        // Measure time taken to read an element from the list
        start = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            list.get(new Random().nextInt(size));
        }
        end = System.currentTimeMillis();
        System.out.println("Time to read: " + (System.currentTimeMillis() - start) + " ms");
    }
}
