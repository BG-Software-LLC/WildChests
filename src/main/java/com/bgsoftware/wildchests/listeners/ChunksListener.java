package com.bgsoftware.wildchests.listeners;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ChunksListener implements Listener {

    private final WildChestsPlugin plugin;

    public ChunksListener(WildChestsPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent e){
        Location chunkLocation = new Location(e.getWorld(), e.getChunk().getX() << 4, 100, e.getChunk().getZ() << 4);
        plugin.getChestsManager().getChests().stream()
                .filter(chest -> LocationUtils.isSameChunk(chest.getLocation(), chunkLocation))
                .forEach(plugin.getNMSInventory()::updateTileEntity);
    }

}
