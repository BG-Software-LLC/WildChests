package com.bgsoftware.wildchests.listeners;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class ChunksListener implements Listener {

    private final WildChestsPlugin plugin;

    public ChunksListener(WildChestsPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent e){
        plugin.getChestsManager().getChests(e.getChunk()).forEach(chest -> {
            if(chest.getLocation().getBlock().getType() != Material.CHEST){
                plugin.getChestsManager().removeChest(chest);
            }
            else{
                plugin.getNMSInventory().updateTileEntity(chest);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent e){
        plugin.getDataHandler().saveDatabase(e.getChunk());
    }

}
