package com.bgsoftware.wildchests.api.events;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class SellChestTaskEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Chest chest;
    private final List<ItemStack> items;
    private boolean cancelled = false;
    private double multiplier;

    public SellChestTaskEvent(Chest chest, List<ItemStack> items, double multiplier){
        this.chest = chest;
        this.items = items;
        this.multiplier = multiplier;
    }

    public Chest getChest() {
        return chest;
    }

    public List<ItemStack> getItems() {
        return items;
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
