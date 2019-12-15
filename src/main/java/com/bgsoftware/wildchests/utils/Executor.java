package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.Bukkit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Executor {

    private static ExecutorService dataService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder().setNameFormat("WildChests DB Thread - #%d").build());
    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    public static void sync(Runnable runnable){
        sync(runnable, 0L);
    }

    public static void sync(Runnable runnable, long delay){
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    public static void async(Runnable runnable){
        if(Bukkit.isPrimaryThread()){
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
        else{
            runnable.run();
        }
    }

    public static void async(Runnable runnable, long delay){
        if(Bukkit.isPrimaryThread()){
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
        }
        else{
            runnable.run();
        }
    }

    public static void data(Runnable runnable){
        dataService.execute(runnable);
    }

    public static void stop(){
        try{
            dataService.shutdown();
            dataService.awaitTermination(1, TimeUnit.MINUTES);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

}
