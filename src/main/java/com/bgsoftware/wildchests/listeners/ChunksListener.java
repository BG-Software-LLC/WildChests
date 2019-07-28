package com.bgsoftware.wildchests.listeners;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ChunksListener implements Listener {

    private WildChestsPlugin plugin;

    public ChunksListener(WildChestsPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e){
        for(BlockState blockState : e.getChunk().getTileEntities()){
            if(blockState instanceof org.bukkit.block.Chest){
                Chest chest = plugin.getChestsManager().getChest(blockState.getLocation());
                if(chest != null)
                    plugin.getNMSAdapter().updateTileEntity(chest);
            }
        }
    }

}
