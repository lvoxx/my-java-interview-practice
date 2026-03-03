package io.github.lvoxx.heapflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the GC promotion mechanism in Java.
 * 
 * This class simulates a scenario where objects are created in the young
 * generation
 * and some are promoted to the old generation based on their survival through
 * garbage
 * collection cycles.
 * 
 * The main method creates approximately 1MB byte arrays in a loop. Every third
 * object
 * is retained in a static list, causing it to survive garbage collection and
 * eventually
 * be promoted to the old generation. The other objects are eligible for
 * collection
 * during young generation garbage collection.
 * 
 * Usage:
 * Run with JVM arguments to observe GC behavior:
 * -Xmx512m -Xms512m (set heap size)
 * -XX:+PrintGCDetails (print GC details)
 * -XX:+PrintGCTimeStamps (print GC timestamps)
 * 
 * @author [Your Name]
 * @version 1.0
 */
public class GCPromotionDemo {
    /** Static list holding references to long-lived objects */
    static List<byte[]> holder = new ArrayList<>();

    /**
     * Main method that creates byte arrays and retains every third one.
     * 
     * @param args command line arguments (not used)
     * @throws InterruptedException if thread sleep is interrupted
     */
    public static void main(String[] args) throws Exception {
        // Implementation...
        for (int i = 0; i < 50; i++) {

            // Tạo object ~1MB
            byte[] data = new byte[1024 * 1024];

            // Giữ lại một số object để chúng sống lâu
            if (i % 3 == 0) {
                holder.add(data);
            }

            Thread.sleep(100);
        }
    }
}
