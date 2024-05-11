package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.Bukkit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Executor {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
    private static final ExecutorService databaseExecutor = Executors.newFixedThreadPool(3,
            new ThreadFactoryBuilder().setNameFormat("WildChests Database Thread %d").build());

    private static boolean shutdown = false;

    public static void sync(Runnable runnable){
        if(shutdown)
            return;

        sync(runnable, 0L);
    }

    public static void sync(Runnable runnable, long delay){
        if(shutdown)
            return;

        Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    public static void async(Runnable runnable){
        if(shutdown)
            return;

        if(Bukkit.isPrimaryThread()){
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
        else{
            runnable.run();
        }
    }

    public static void async(Runnable runnable, long delay){
        if(shutdown)
            return;

        if(Bukkit.isPrimaryThread()){
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
        }
        else{
            runnable.run();
        }
    }

    public static boolean isDataThread(){
        return Thread.currentThread().getName().contains("WildChests Database Thread");
    }

    public static void data(Runnable runnable){
        if(shutdown)
            return;

        databaseExecutor.execute(runnable);
    }

    public static void stop(){
        try{
            shutdown = true;
            WildChestsPlugin.log("Shutting down database executor");
            shutdownAndAwaitTermination();
        }catch(Exception ex){
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
