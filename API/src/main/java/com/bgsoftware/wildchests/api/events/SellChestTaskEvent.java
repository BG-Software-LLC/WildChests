package com.bgsoftware.wildchests.api.events;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public final class SellChestTaskEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Chest chest;
    private final ItemStack item;
    private boolean cancelled = false;
    private double multiplier;

    public SellChestTaskEvent(Chest chest, ItemStack item, double multiplier){
        this.chest = chest;
        this.item = item;
        this.multiplier = multiplier;
    }

    public Chest getChest() {
        return chest;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
