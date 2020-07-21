package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.Bukkit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Executor {

    private static final ExecutorService dataService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder().setNameFormat("WildChests DB Thread - #%d").build());
    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
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

    public static void data(Runnable runnable){
        if(shutdown)
            return;

        dataService.execute(runnable);
    }

    public static void stop(){
        try{
            shutdown = true;
            dataService.shutdown();
            dataService.awaitTermination(1, TimeUnit.MINUTES);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

}
