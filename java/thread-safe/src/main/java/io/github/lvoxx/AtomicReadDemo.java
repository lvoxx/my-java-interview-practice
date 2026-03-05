package io.github.lvoxx;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicReadDemo {
    static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        Runnable runnable = () -> {
            int read = counter.get();
            System.out.println(Thread.currentThread().getName() + " read: " + read);

            // Simulate some work
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Do write
            int updatedValue = counter.incrementAndGet();
            System.out.println(Thread.currentThread().getName() + " wrote: " + updatedValue);
        };

        Thread thread1 = new Thread(runnable, "Thread-1");
        Thread thread2 = new Thread(runnable, "Thread-2");

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        System.out.println("Final counter value: " + counter.get());
    }
}
