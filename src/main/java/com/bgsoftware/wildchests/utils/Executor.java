package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.Bukkit;

public final class Executor {

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

    public static void stop(){
        shutdown = true;
    }

}
