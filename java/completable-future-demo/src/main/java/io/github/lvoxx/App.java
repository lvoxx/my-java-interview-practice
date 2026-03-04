package io.github.lvoxx;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the usage of CompletableFuture for asynchronous operations and
 * chaining.
 * 
 * <p>
 * This application showcases a typical workflow involving:
 * </p>
 * <ul>
 * <li>Fetching user data asynchronously</li>
 * <li>Enriching user information</li>
 * <li>Composing multiple async operations</li>
 * <li>Processing results and handling exceptions</li>
 * </ul>
 * 
 * <p>
 * The main thread initiates an async chain that:
 * <ol>
 * <li>Fetches a user by ID using {@code supplyAsync}</li>
 * <li>Enriches the user data using {@code thenApply}</li>
 * <li>Fetches orders for the user using {@code thenCompose}</li>
 * <li>Processes the retrieved orders using {@code thenAccept}</li>
 * <li>Handles any exceptions that occur during the chain</li>
 * </ol>
 * </p>
 * 
 * <p>
 * The main thread blocks until all async operations complete using
 * {@code join()}.
 * </p>
 * 
 * @author lvoxx
 * @version 1.0
 */
public class App {
    public static void main(String[] args) {
        runOnSameThread();
        System.out.println("\n====================\n");
        runOnDifferentThread();
    }

    public static void runOnSameThread() {
        String id = "u1";

        System.out.println("Demonstrating CompletableFuture with same thread:");
        System.out.println("Main thread: " + Thread.currentThread().getName());

        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> fetchUser(id))
                .thenApply(user -> enrichUser(user))
                .thenCompose(user -> fetchOrder(user.getId()))
                .thenAccept(orders -> process(orders))
                .exceptionally(ex -> {
                    log(ex);
                    return null;
                });

        future.join();

        System.out.println("DONE");
    }

    public static void runOnDifferentThread() {
        String id = "u1";

        System.out.println("Demo CompletableFuture with different threads:");
        System.out.println("Main thread: " + Thread.currentThread().getName());

        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> fetchUser(id))
                .thenApply(user -> enrichUser(user))
                .thenComposeAsync(user -> fetchOrder(user.getId())) // Summit new task to the common pool by
                                                                    // ForkJoinPool.commonPool()
                .thenAccept(orders -> process(orders))
                .exceptionally(ex -> {
                    log(ex);
                    return null;
                });

        future.join();

        System.out.println("DONE");
    }

    // ================== Simulate a long-running operation ==================

    public static User fetchUser(String id) {
        squeezeTheThread(1);
        System.out.println("Fetched user in thread: " + Thread.currentThread().getName());
        return new User(id, "John Doe");
    }

    public static User enrichUser(User user) {
        squeezeTheThread(1);
        System.out.println("Enriched user in thread: " + Thread.currentThread().getName());
        user.setName(user.getName() + " (Enriched)");
        return user;
    }

    public static CompletableFuture<List<Order>> fetchOrder(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            squeezeTheThread(1);
            System.out.println(
                    "Fetched order for user " + userId + " in thread: " + Thread.currentThread().getName());
            return List.of(new Order("o1"), new Order("o2"), new Order("o3"));
        });
    }

    static void process(List<Order> orders) {
        System.out.println("process in " + Thread.currentThread().getName());
        orders.forEach(o -> System.out.println("Processed: " + o.getId()));
    }

    static void log(Throwable ex) {
        System.err.println("Error: " + ex.getMessage());
    }

    public static void squeezeTheThread(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
