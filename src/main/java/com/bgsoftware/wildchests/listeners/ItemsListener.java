package com.bgsoftware.wildchests.listeners;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class ItemsListener implements Listener {

    private WildChestsPlugin plugin;

    public ItemsListener(WildChestsPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent e){
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if(e.getEntity().isValid() && !e.getEntity().isDead()) {
                if(!e.getEntity().isOnGround()) {
                    onItemSpawn(e);
                    return;
                }
                trySuction(e.getEntity());
            }
        }, 20L);
    }

    private void trySuction(Item item){
        List<Chest> chestList = plugin.getChestsManager().getNearbyChests(item.getLocation());
        for (Chest chest : chestList) {
            if (chest.addItems(item.getItemStack()).isEmpty()) {
                item.remove();
                break;
            }
        }
    }

}
