package com.bgsoftware.wildchests.hooks;

import com.Acrobot.ChestShop.Events.PreShopCreationEvent;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
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
                        if(blockData instanceof WallSign || blockData instanceof Sign){
                            for(Location location : getPotentialChests(e.getSign().getBlock())){
                                Chest chest = plugin.getChestsManager().getChest(location);
                                if(chest != null) {
                                    e.setCancelled(true);
                                    e.setSignLines(new String[] {"", "", "", ""});
                                    break;
                                }
                            }
                        }
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }

            }, plugin);
        }
    }

    private static Location[] getPotentialChests(Block block){
        return new Location[] {
                block.getLocation().add(1, 0, 0),
                block.getLocation().add(0, 1, 0),
                block.getLocation().add(0, 0, 1),
                block.getLocation().add(-1, 0, 0),
                block.getLocation().add(0, -1, 0),
                block.getLocation().add(0, 0, -1)
        };
    }

}
