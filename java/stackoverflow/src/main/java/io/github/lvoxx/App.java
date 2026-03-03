package io.github.lvoxx;

/**
 * Hello world!
 *
 */
public class App {
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
