package com.bgsoftware.wildchests.api.objects.chests;

import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;

public interface StorageChest extends RegularChest, Chest {

    ItemStack getItemStack();

    void setItemStack(ItemStack itemStack);

    BigInteger getAmount();

    default BigInteger getExactAmount(){
        return getAmount();
    }

    void setAmount(BigInteger amount);

    BigInteger getMaxAmount();

    void setMaxAmount(BigInteger maxAmount);

}
