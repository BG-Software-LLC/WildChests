package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DatabaseThread {
    private static final ExecutorService databaseExecutor = Executors.newFixedThreadPool(3,
            new ThreadFactoryBuilder().setNameFormat("WildChests Database Thread %d").build());

    private static boolean shutdown = false;

    public static boolean isDataThread() {
        return Thread.currentThread().getName().contains("WildChests Database Thread");
    }

    public static void schedule(Runnable runnable) {
        if (shutdown)
            return;

        databaseExecutor.execute(runnable);
    }

    public static void stop() {
        try {
            shutdown = true;
            WildChestsPlugin.log("Shutting down database executor");
            shutdownAndAwaitTermination();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void shutdownAndAwaitTermination() {
        databaseExecutor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!databaseExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                databaseExecutor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!databaseExecutor.awaitTermination(60, TimeUnit.SECONDS))
                    WildChestsPlugin.log("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            databaseExecutor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

}
