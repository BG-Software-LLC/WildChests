package com.bgsoftware.wildchests.objects.inventory;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public interface WildContainerItem {

    WildContainerItem AIR = WildChestsPlugin.getPlugin().getNMSInventory().createItemStack(new ItemStack(Material.AIR));

    ItemStack getBukkitItem();

    WildContainerItem copy();

}
