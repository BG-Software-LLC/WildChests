package com.bgsoftware.wildchests.hooks;

import com.Acrobot.ChestShop.Events.PreShopCreationEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;

public final class ChestShopHook {

    private static Method GET_BLOCK_DATA;

    static {
        try{
            GET_BLOCK_DATA = BlockState.class.getMethod("getBlockData");
        }catch (Exception ignored){}
    }

    public static void register(WildChestsPlugin plugin){
        if(GET_BLOCK_DATA != null) {
            Bukkit.getPluginManager().registerEvents(new Listener() {

                @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
                public void onChestShopCreate(PreShopCreationEvent e) {
                    try {
                        BlockData blockData = (BlockData) GET_BLOCK_DATA.invoke(e.getSign());
                        if(blockData instanceof Directional){
                            Block attachedBlock = e.getSign().getBlock().getRelative(
                                    ((Directional) blockData).getFacing().getOppositeFace());

                            Chest chest = plugin.getChestsManager().getChest(attachedBlock.getLocation());

                            if(chest != null) {
                                e.setCancelled(true);
                                e.setSignLines(new String[] {"", "", "", ""});
                            }
                        }
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }

            }, plugin);
        }
    }

}
