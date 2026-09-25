package com.bgsoftware.wildchests.nms.v26_3.utils;

import com.bgsoftware.common.reflection.ReflectMethod;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

public class NMSUtils {

    private static final ReflectMethod<org.bukkit.inventory.ItemStack> CRAFT_ITEM_STACK_AS_CRAFT_MIRROR = new ReflectMethod<>(
            CraftItemStack.class, org.bukkit.inventory.ItemStack.class, "asCraftMirror", ItemStack.class);

    private static final ReflectMethod<ItemStack> CRAFT_ITEM_STACK_COPY_NMS_STACK = new ReflectMethod<>(
            CraftItemStack.class, ItemStack.class, "copyNMSStack", ItemStack.class, int.class);

    private NMSUtils() {

    }

    public static org.bukkit.inventory.ItemStack asMirror(ItemStack itemStack) {
        // Spigot still uses the CraftItemStack#asCraftMirror(ItemStack).
        if (CRAFT_ITEM_STACK_AS_CRAFT_MIRROR.isValid()) {
            return CRAFT_ITEM_STACK_AS_CRAFT_MIRROR.invoke(null, itemStack);
        }

        return CraftItemStack.asBukkitMirror(itemStack);
    }

    public static ItemStack copyNMSStack(ItemStack original, int amount) {
        // Spigot still uses the CraftItemStack#copyNMSStack(ItemStack, int).
        if (CRAFT_ITEM_STACK_COPY_NMS_STACK.isValid()) {
            return CRAFT_ITEM_STACK_COPY_NMS_STACK.invoke(null, original, amount);
        }

        return original.copyWithCount(amount);
    }

}