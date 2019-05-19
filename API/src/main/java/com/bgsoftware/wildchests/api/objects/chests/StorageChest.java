package com.bgsoftware.wildchests.api.objects.chests;

import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;

public interface StorageChest extends RegularChest, Chest {

    ItemStack getItemStack();

    void setItemStack(ItemStack itemStack);

    @Deprecated
    int getAmount();

    BigInteger getExactAmount();

    void setAmount(int amount);

    void setAmount(BigInteger amount);

}
