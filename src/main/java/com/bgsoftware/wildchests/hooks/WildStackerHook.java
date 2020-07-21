package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public final class WildStackerHook {

    private static final boolean enabled = Bukkit.getPluginManager().isPluginEnabled("WildStacker");

    public static ItemStack getItemStack(Item item){
        return WildStackerAPI.getStackedItem(item).getItemStack();
    }

    public static int getItemAmount(Item item){
        return WildStackerAPI.getItemAmount(item);
    }

    public static void setRemainings(Item item, int remainings){
        WildStackerAPI.getStackedItem(item).setStackAmount(remainings, true);
    }

    public static void dropItem(Location location, ItemStack itemStack, int amount){
        WildStackerAPI.getWildStacker().getSystemManager().spawnItemWithAmount(location, itemStack, amount);
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
