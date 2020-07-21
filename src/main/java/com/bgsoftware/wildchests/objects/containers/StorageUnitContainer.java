package com.bgsoftware.wildchests.objects.containers;

import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;

public interface StorageUnitContainer extends InventoryContainer {

    ItemStack getItemStack();

    void setItemStack(ItemStack itemStack);

    BigInteger getAmount();

    void setAmount(BigInteger amount);

    BigInteger getMaxAmount();

    void setMaxAmount(BigInteger maxAmount);

}
