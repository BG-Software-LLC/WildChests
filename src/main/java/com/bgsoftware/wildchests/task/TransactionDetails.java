package com.bgsoftware.wildchests.task;

import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

public class TransactionDetails {

    private final ItemStack itemStack;
    private int amount;
    private BigDecimal amountEarned;

    public TransactionDetails(ItemStack itemStack, int amount, BigDecimal amountEarned) {
        this.itemStack = itemStack;
        this.amount = amount;
        this.amountEarned = amountEarned;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getAmount() {
        return amount;
    }

    public BigDecimal getEarnings() {
        return amountEarned;
    }

    public void increaseAmount(int amount) {
        this.amount += amount;
    }

    public void increaseEarnings(BigDecimal balance) {
        amountEarned = amountEarned.add(balance);
    }

}
