package com.bgsoftware.wildchests.task;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;

public final class SaveTask extends BukkitRunnable {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private static int taskID = -1;

    private SaveTask(){
        if(plugin.getSettings().saveInterval > 0)
            taskID = runTaskTimerAsynchronously(plugin, plugin.getSettings().saveInterval, plugin.getSettings().saveInterval).getTaskId();
    }

    public static void start(){
        if(Bukkit.getScheduler().isCurrentlyRunning(taskID))
            Bukkit.getScheduler().cancelTask(taskID);
        new SaveTask();
    }

    @Override
    public void run() {
        if(Bukkit.getOnlinePlayers().size() > 0) {
            plugin.getDataHandler().saveDatabase();
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.isOp())
                    Locale.AUTO_SAVE.send(pl);
            }
        }
    }
}
