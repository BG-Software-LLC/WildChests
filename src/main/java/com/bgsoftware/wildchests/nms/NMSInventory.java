package com.bgsoftware.wildchests.nms;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.objects.inventory.WildContainerItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface NMSInventory {

    void updateTileEntity(Chest chest);

    void removeTileEntity(Chest chest);

    WildContainerItem createItemStack(ItemStack itemStack);

    CraftWildInventory createInventory(Chest chest, int size, String title, int index);

    void openPage(Player player, CraftWildInventory inventory);

    void createDesignItem(CraftWildInventory inventory, ItemStack itemStack);

}
