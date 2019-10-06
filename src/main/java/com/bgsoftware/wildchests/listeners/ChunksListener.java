package com.bgsoftware.wildchests.listeners;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.task.HopperTask;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

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
                if(chest != null) {
                    plugin.getNMSAdapter().updateTileEntity(chest);
                    Block hopperBlock = blockState.getBlock().getRelative(BlockFace.DOWN);
                    if(hopperBlock.getState() instanceof Hopper)
                        HopperTask.addHopper(chest, (Hopper) hopperBlock.getState());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent e){
        for(BlockState blockState : e.getChunk().getTileEntities()){
            if(blockState instanceof org.bukkit.block.Chest){
                Chest chest = plugin.getChestsManager().getChest(blockState.getLocation());
                if(chest != null)
                    HopperTask.removeHopper(chest);
            }
        }
    }

}
