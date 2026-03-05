package io.github.lvoxx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentReadDemo {
    static ConcurrentMap<String, Integer> counter = new ConcurrentHashMap<>(Map.of("readme", 1));

    public static void main(String[] args) throws InterruptedException {
        Runnable runnable = () -> {
            int read = counter.get("readme");
            System.out.println(Thread.currentThread().getName() + " read: " + read);

            // Simulate some work
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Do write
            counter.put("readme", read + 1);
            System.out.println(Thread.currentThread().getName() + " wrote: " + counter.get("readme"));
        };

        Thread thread1 = new Thread(runnable, "Thread-1");
        Thread thread2 = new Thread(runnable, "Thread-2");

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        System.out.println("Final counter value: " + counter.get("readme"));
    }
}
