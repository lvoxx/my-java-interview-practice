package io.github.lvoxx;

public class ThreadSafeAsyncDemo {
    static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        Runnable runnable = () -> {
            synchronized (ThreadSafeAsyncDemo.class) {
                int read = counter;
                System.out.println(Thread.currentThread().getName() + " read: " + read);

                // Simulate some work
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Do write
                counter = read + 1;
                System.out.println(Thread.currentThread().getName() + " wrote: " + counter);
            }
        };

        Thread thread1 = new Thread(runnable, "Thread-1");
        Thread thread2 = new Thread(runnable, "Thread-2");

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
    }
}
