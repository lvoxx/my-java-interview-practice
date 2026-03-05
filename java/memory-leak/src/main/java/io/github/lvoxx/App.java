package io.github.lvoxx;

import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        List<Object> cache = new ArrayList<>();

        try {
            while (true) {
                cache.add(new byte[1024 * 1024]);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("Out of memory error caught: " + e.getMessage());
        }
    }
}
