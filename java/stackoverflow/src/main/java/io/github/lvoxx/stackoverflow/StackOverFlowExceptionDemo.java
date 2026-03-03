package io.github.lvoxx.stackoverflow;

/**
 * Demonstrates a StackOverflowError in Java.
 * 
 * This class contains a recursive method that calls itself indefinitely,
 * leading
 * to a stack overflow. The main method catches the StackOverflowError and
 * prints the number of recursive calls made before the error occurred.
 * 
 * Usage:
 * Run with JVM arguments to observe stack behavior:
 * -Xss1m (set stack size to 1MB)
 * 
 * @author Lvoxx
 * @version 1.0
 */
public class StackOverFlowExceptionDemo {
    static int count = 0;

    static void recursive() {
        int x = count++; // local
        recursive();
    }

    public static void main(String[] args) {
        try {
            recursive();
        } catch (StackOverflowError e) {
            System.out.println("Stack overflow occurred after " + count + " recursive calls.");
        }

    }
}
