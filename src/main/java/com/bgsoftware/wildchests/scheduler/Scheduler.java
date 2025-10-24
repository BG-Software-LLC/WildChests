package com.bgsoftware.wildchests.scheduler;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class Scheduler {
    private static final ISchedulerImplementation IMP = initializeSchedulerImplementation();

    private static ISchedulerImplementation initializeSchedulerImplementation() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
        } catch (ClassNotFoundException error) {
            return BukkitSchedulerImplementation.INSTANCE;
        }

        // Detected Folia, create its scheduler
        try {
            Class<?> foliaSchedulerClass = Class.forName("com.bgsoftware.wildchests.scheduler.FoliaSchedulerImplementation");
            return (ISchedulerImplementation) foliaSchedulerClass.getField("INSTANCE").get(null);
        } catch (Throwable error) {
            throw new RuntimeException(error);
        }
    }

    private Scheduler() {

    }

    public static void initialize() {
        // Do nothing, load static initializer
    }

    public static void cancelTasks() {
        IMP.cancelTasks();
    }

    public static ScheduledTask runTask(World world, int chunkX, int chunkZ, Runnable task, long delay) {
        return IMP.scheduleTask(world, chunkX, chunkZ, task, delay);
    }

    public static ScheduledTask runTask(Entity entity, Runnable task, long delay) {
        return IMP.scheduleTask(entity, task, delay);
    }

    public static ScheduledTask runTask(Runnable task, long delay) {
        return IMP.scheduleTask(task, delay);
    }

    public static ScheduledTask runTaskAsync(Runnable task, long delay) {
        return IMP.scheduleAsyncTask(task, delay);
    }

    public static ScheduledTask runRepeatingTaskAsync(Runnable task, long delay) {
        return IMP.scheduleRepeatingAsyncTask(task, delay);
    }

    public static ScheduledTask runTask(Chunk chunk, Runnable task, long delay) {
        return IMP.scheduleTask(chunk.getWorld(), chunk.getX(), chunk.getZ(), task, delay);
    }

    public static boolean isScheduledForRegion(World world, int chunkX, int chunkZ) {
        return IMP.isScheduledForRegion(world, chunkX, chunkZ);
    }

    public static boolean isScheduledForRegion(Entity entity) {
        return IMP.isScheduledForRegion(entity);
    }

    public static ScheduledTask runTask(Chunk chunk, Runnable task) {
        return runTask(chunk, task, 0L);
    }

    public static ScheduledTask runTask(Location location, Runnable task, long delay) {
        return runTask(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task, delay);
    }

    public static ScheduledTask runTask(Entity entity, Runnable task) {
        return runTask(entity, task, 0L);
    }

    public static ScheduledTask runTask(Location location, Runnable task) {
        return runTask(location, task, 0L);
    }

    public static ScheduledTask runTask(World world, int chunkX, int chunkZ, Runnable task) {
        return runTask(world, chunkX, chunkZ, task, 0L);
    }

    public static ScheduledTask runTask(Runnable task) {
        return runTask(task, 0L);
    }

    public static ScheduledTask runTaskAsync(Runnable task) {
        return runTaskAsync(task, 0L);
    }

    public static boolean isScheduledForRegion(Chunk chunk) {
        return isScheduledForRegion(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    public static boolean isScheduledForRegion(Location location) {
        return isScheduledForRegion(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }


    public static void ensureMain(World world, int chunkX, int chunkZ, Runnable runnable) {
        if (isScheduledForRegion(world, chunkX, chunkZ)) {
            runnable.run();
        } else {
            runTask(world, chunkX, chunkZ, runnable);
        }
    }

    public static void ensureMain(Entity entity, Runnable runnable) {
        if (isScheduledForRegion(entity)) {
            runnable.run();
        } else {
            runTask(entity, runnable);
        }
    }
    public static void ensureMain(Location location, Runnable runnable) {
        if (isScheduledForRegion(location)) {
            runnable.run();
        } else {
            runTask(location, runnable);
        }
    }

}
