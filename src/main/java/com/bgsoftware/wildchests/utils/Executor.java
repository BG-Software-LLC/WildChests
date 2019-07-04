package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.Bukkit;

public final class Executor {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    public static void sync(Runnable runnable){
        sync(runnable, 0L);
    }

    public static void sync(Runnable runnable, long delay){
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    public static void async(Runnable runnable){
        async(runnable, 0L);
    }

    public static void async(Runnable runnable, long delay){
        if(Bukkit.isPrimaryThread()){
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
        }
        else{
            runnable.run();
        }
    }

}
