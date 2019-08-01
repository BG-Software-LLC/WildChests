package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public final class WildStackerHook {

    public static ItemStack getItemStack(Item item){
        return WildStackerAPI.getStackedItem(item).getItemStack();
    }

    public static void setRemainings(Item item, int remainings){
        WildStackerAPI.getStackedItem(item).setStackAmount(remainings, true);
    }

}
