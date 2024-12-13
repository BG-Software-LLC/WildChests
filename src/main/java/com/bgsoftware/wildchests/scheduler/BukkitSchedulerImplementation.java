package com.bgsoftware.wildchests.scheduler;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

public class BukkitSchedulerImplementation implements ISchedulerImplementation {

    public static final BukkitSchedulerImplementation INSTANCE = new BukkitSchedulerImplementation();

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private BukkitSchedulerImplementation() {

    }

    @Override
    public boolean isRegionScheduler() {
        return false;
    }

    @Override
    public void cancelTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public ScheduledTask scheduleTask(World world, int chunkX, int chunkZ, Runnable task, long delay) {
        return scheduleTask(task, delay);
    }

    @Override
    public ScheduledTask scheduleTask(Entity unused, Runnable task, long delay) {
        return scheduleTask(task, delay);
    }

    @Override
    public ScheduledTask scheduleTask(Runnable task, long delay) {
        if (delay <= 0) {
            return new BukkitScheduledTask(Bukkit.getScheduler().runTask(plugin, task));
        } else {
            return new BukkitScheduledTask(Bukkit.getScheduler().runTaskLater(plugin, task, delay));
        }
    }

    @Override
    public ScheduledTask scheduleAsyncTask(Runnable task, long delay) {
        if (delay <= 0) {
            return new BukkitScheduledTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
        } else {
            return new BukkitScheduledTask(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay));
        }
    }

    @Override
    public ScheduledTask scheduleRepeatingAsyncTask(Runnable task, long delay) {
        return new BukkitScheduledTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, delay));
    }

    @Override
    public boolean isScheduledForRegion(World world, int chunkX, int chunkZ) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isScheduledForRegion(Entity entity) {
        return Bukkit.isPrimaryThread();
    }

    private static class BukkitScheduledTask implements ScheduledTask {

        private int taskId;

        BukkitScheduledTask(BukkitTask bukkitTask) {
            this(bukkitTask.getTaskId());
        }

        BukkitScheduledTask(int taskId) {
            this.taskId = taskId;
        }

        @Override
        public void cancel() {
            if (Bukkit.getScheduler().isCurrentlyRunning(this.taskId)) {
                Bukkit.getScheduler().cancelTask(this.taskId);
                this.taskId = -1;
            }
        }
    }

}
