package com.bgsoftware.wildchests.nms.v1_20_3.utils;

import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;

public class NMSUtils {

    private NMSUtils() {

    }

    public static org.bukkit.inventory.ItemStack asMirror(ItemStack itemStack) {
        return CraftItemStack.asCraftMirror(itemStack);
    }

    public static ItemStack copyNMSStack(ItemStack original, int amount) {
        return CraftItemStack.copyNMSStack(original, amount);
    }

}