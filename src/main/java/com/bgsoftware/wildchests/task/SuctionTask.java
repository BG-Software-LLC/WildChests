package com.bgsoftware.wildchests.task;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.objects.WLocation;
import com.bgsoftware.wildchests.utils.ChestUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.stream.Stream;

public final class SuctionTask extends BukkitRunnable {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private WLocation location;
    private int taskId = -1;

    public SuctionTask(WLocation location){
        this.location = location;
        Bukkit.getScheduler().runTask(plugin, this::start);
    }

    @Override
    public void run() {
        if(!location.isChunkLoaded())
            return;

        Chest chest = plugin.getChestsManager().getChest(location.getLocation());

        if(chest == null || !chest.getData().isAutoSuction()){
            cancel();
            return;
        }

        getNearbyItems(chest).forEach(ChestUtils::trySuctionChest);
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

    private Stream<Item> getNearbyItems(Chest chest){
        ChestData chestData = chest.getData();
        return plugin.getNMSAdapter().getNearbyItems(chest.getLocation(), chestData.getAutoSuctionRange(), chestData.isAutoSuctionChunk());
    }

}
