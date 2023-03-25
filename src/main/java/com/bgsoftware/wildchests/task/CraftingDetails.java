package com.bgsoftware.wildchests.task;

import org.bukkit.inventory.ItemStack;

public class CraftingDetails {

    private final ItemStack itemStack;
    private int amount;

    public CraftingDetails(ItemStack itemStack, int amount) {
        this.itemStack = itemStack;
        this.amount = amount;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getAmount() {
        return amount;
    }

    public void increaseAmount(int amount) {
        this.amount += amount;
    }

}
