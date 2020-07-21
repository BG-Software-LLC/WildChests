package com.bgsoftware.wildchests.objects.inventory;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface CraftWildInventory extends Inventory {

    Chest getOwner();

    WildItemStack<?, ? extends ItemStack> getWildItem(int slot);

    void setItem(int i, WildItemStack<?, ?> itemStack);

    WildItemStack<?, ?>[] getWildContents();

    void setTitle(String title);

    String getTitle();

    boolean isFull();

}
