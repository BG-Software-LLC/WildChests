package com.bgsoftware.wildchests.api.objects.chests;

import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;


/**
 * StorageChests (Storage-Units) are chests that can hold only one item, but with unlimited amount of it.
 */
public interface StorageChest extends RegularChest, Chest {

    /**
     * Get the stored item of this chest.
     * If the chest is empty, AIR item will be returned.
     */
    ItemStack getItemStack();

    /**
     * Change the item that is stored in this chest.
     * @param itemStack The new item to store.
     */
    void setItemStack(ItemStack itemStack);

    /**
     * Get the amount of the stored item.
     */
    BigInteger getAmount();

    /**
     * Get the amount of the stored item.
     * @deprecated Use getAmount()
     */
    @Deprecated
    BigInteger getExactAmount();

    /**
     * Set the amount of the stored item.
     * @param amount The new amount of the item.
     */
    void setAmount(BigInteger amount);

    /**
     * Set the amount of the stored item.
     * @param amount The new amount of the item.
     * @deprecated Use setAmount(BigInteger amount)
     */
    @Deprecated
    void setAmount(int amount);

    /**
     * Get the maximum amount that can be stored.
     * If there's no limit, -1 will be returned.
     */
    BigInteger getMaxAmount();

    /**
     * Set the maximum amount of the item that can be stored.
     * @param maxAmount The maximum amount
     */
    void setMaxAmount(BigInteger maxAmount);

    /**
     * Update the inventory for all the viewers.
     * Used when the title is changed so it will be updated to all the viewers.
     */
    void update();

}
