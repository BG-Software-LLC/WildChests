package com.bgsoftware.wildchests.api.events;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * This event is fired when a chest attempts to sell an item.
 */
public final class SellChestTaskEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Chest chest;
    private final ItemStack item;
    private boolean cancelled = false;
    private double multiplier;

    /**
     * The constructor of the event.
     * @param chest The chest that sells the item.
     * @param item The item to be sold.
     * @param multiplier The applied price multiplier.
     */
    public SellChestTaskEvent(Chest chest, ItemStack item, double multiplier){
        this.chest = chest;
        this.item = item;
        this.multiplier = multiplier;
    }

    /**
     * Get the chest that sells the item.
     */
    public Chest getChest() {
        return chest;
    }

    /**
     * Get the item that is sold.
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Get the applied price multiplier.
     */
    public double getMultiplier() {
        return multiplier;
    }

    /**
     * Set a price multiplier.
     * @param multiplier The new price multiplier.
     */
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
