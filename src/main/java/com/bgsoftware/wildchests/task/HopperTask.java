package com.bgsoftware.wildchests.task;

import com.bgsoftware.wildchests.objects.WLocation;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;

import java.util.Map;

public final class HopperTask extends BukkitRunnable {

    private static Map<Chest, Hopper> hopperMap = Maps.newConcurrentMap();
    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private WLocation location;
    private int taskId = -1;

    public HopperTask(WLocation location){
        this.location = location;
        Bukkit.getScheduler().runTask(plugin, this::start);
    }

    @Override
    public void run() {
        if(!location.isChunkLoaded())
            return;

        Chest chest = plugin.getChestsManager().getChest(location.getLocation());

        if(chest == null){
            cancel();
            return;
        }

        Inventory hopperInventory = getHopperInventory(chest);

        if(hopperInventory == null)
            return;

        chest.onHopperItemTake(hopperInventory);
    }

    public void stop(){
        Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    public void start(){
        if(taskId != -1)
            stop();

        int hopperTransfer = plugin.getNMSAdapter().getHopperTransfer(location.getWorld());
        taskId = runTaskTimerAsynchronously(plugin, hopperTransfer, hopperTransfer).getTaskId();
    }

    private Inventory getHopperInventory(Chest chest){
        if(hopperMap.containsKey(chest))
            return hopperMap.get(chest).getInventory();

        Location hopperBlock = chest.getLocation().subtract(0, 1, 0);

        try {
            for (Entity entity : chest.getLocation().getChunk().getEntities()) {
                if (entity instanceof HopperMinecart && entity.getLocation().getBlock().getLocation().equals(hopperBlock)) {
                    return ((HopperMinecart) entity).getInventory();
                }
            }
        }catch(Exception ignored){}

        return null;
    }

    public static void addHopper(Chest chest, Hopper hopper){
        hopperMap.put(chest, hopper);
    }

    public static void removeHopper(Chest chest){
        hopperMap.remove(chest);
    }

}
