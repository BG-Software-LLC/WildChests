package com.bgsoftware.wildchests.api.objects.chests;

import org.bukkit.inventory.ItemStack;

public interface StorageChest extends RegularChest, Chest {

    ItemStack getItemStack();

    void setItemStack(ItemStack itemStack);

    int getAmount();

    void setAmount(int amount);

}
