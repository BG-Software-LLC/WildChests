package com.bgsoftware.wildchests.listeners;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

import java.util.List;

public final class ItemsListener implements Listener {

    private WildChestsPlugin plugin;

    public ItemsListener(WildChestsPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent e){
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            List<Chest> chestList = plugin.getChestsManager().getNearbyChests(e.getLocation());
            for(Chest chest : chestList){
                if(chest.addItems(e.getEntity().getItemStack()).isEmpty()){
                    e.getEntity().remove();
                    break;
                }
            }
        }, 20L);
    }

}
