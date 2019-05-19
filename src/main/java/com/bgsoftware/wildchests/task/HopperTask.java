package com.bgsoftware.wildchests.task;

import com.bgsoftware.wildchests.objects.WLocation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;

public final class HopperTask extends BukkitRunnable {

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

        Inventory hopperInventory = getHopperInventory(location.getLocation().getBlock().getRelative(BlockFace.DOWN));

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

    private Inventory getHopperInventory(Block hopperBlock){
        if(hopperBlock.getType() == Material.HOPPER)
            return ((Hopper) hopperBlock.getState()).getInventory();

        try {
            for (Entity entity : hopperBlock.getLocation().getChunk().getEntities()) {
                if (entity instanceof HopperMinecart && entity.getLocation().getBlock().equals(hopperBlock)) {
                    return ((HopperMinecart) entity).getInventory();
                }
            }
        }catch(Exception ignored){}

        return null;
    }

}
