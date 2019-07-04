package com.bgsoftware.wildchests.listeners;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.utils.ChestUtils;
import com.bgsoftware.wildchests.utils.Executor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

@SuppressWarnings({"unused", ""})
public final class ItemsListener implements Listener {

    private WildChestsPlugin plugin;

    public ItemsListener(WildChestsPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent e){
        Executor.async(() -> ChestUtils.trySuctionChest(e.getEntity()), 20L);
    }

}
