package com.bgsoftware.wildchests.scheduler;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.concurrent.TimeUnit;

public class FoliaSchedulerImplementation implements ISchedulerImplementation {

    public static final FoliaSchedulerImplementation INSTANCE = new FoliaSchedulerImplementation();

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private FoliaSchedulerImplementation() {
    }

    @Override
    public boolean isRegionScheduler() {
        return true;
    }

    @Override
    public void cancelTasks() {
        Bukkit.getServer().getAsyncScheduler().cancelTasks(plugin);
    }

    @Override
    public ScheduledTask scheduleTask(World world, int chunkX, int chunkZ, Runnable task, long delay) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask handle;
        if (delay <= 0) {
            handle = Bukkit.getServer().getRegionScheduler().run(plugin, world, chunkX, chunkZ, v -> task.run());
        } else {
            handle = Bukkit.getServer().getRegionScheduler().runDelayed(plugin, world, chunkX, chunkZ, v -> task.run(), delay);
        }
        return new FoliaScheduledTask(handle);
    }

    @Override
    public ScheduledTask scheduleTask(Entity entity, Runnable task, long delay) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask handle;
        if (delay <= 0) {
            handle = entity.getScheduler().run(plugin, v -> task.run(), task);
        } else {
            handle = entity.getScheduler().runDelayed(plugin, v -> task.run(), task, delay);
        }
        return new FoliaScheduledTask(handle);
    }

    @Override
    public ScheduledTask scheduleTask(Runnable task, long delay) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask handle;
        if (delay <= 0) {
            handle = Bukkit.getServer().getGlobalRegionScheduler().run(plugin, v -> task.run());
        } else {
            handle = Bukkit.getServer().getGlobalRegionScheduler().runDelayed(plugin, v -> task.run(), delay);
        }

        return new FoliaScheduledTask(handle);
    }

    @Override
    public ScheduledTask scheduleAsyncTask(Runnable task, long delay) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask handle;
        if (delay <= 0) {
            handle = Bukkit.getServer().getAsyncScheduler().runNow(plugin, v -> task.run());
        } else {
            handle = Bukkit.getServer().getAsyncScheduler().runDelayed(plugin, v -> task.run(), delay * 50L, TimeUnit.MILLISECONDS);
        }

        return new FoliaScheduledTask(handle);
    }

    @Override
    public ScheduledTask scheduleRepeatingAsyncTask(Runnable task, long delay) {
        return new FoliaScheduledTask(Bukkit.getServer().getAsyncScheduler().runAtFixedRate(
                plugin, v -> task.run(), delay * 50L, delay * 50L, TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean isScheduledForRegion(World world, int chunkX, int chunkZ) {
        return Bukkit.getServer().isOwnedByCurrentRegion(world, chunkX, chunkZ);
    }

    @Override
    public boolean isScheduledForRegion(Entity entity) {
        return Bukkit.getServer().isOwnedByCurrentRegion(entity);
    }

    private static class FoliaScheduledTask implements ScheduledTask {

        private final io.papermc.paper.threadedregions.scheduler.ScheduledTask handle;

        public FoliaScheduledTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask handle) {
            this.handle = handle;
        }

        @Override
        public void cancel() {
            if (!handle.isCancelled())
                handle.cancel();
        }
    }

}
