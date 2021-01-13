package com.bgsoftware.wildchests.api.hooks;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public interface StackerProvider {

    /**
     * Get the amount of a dropped item.
     * @param item The item to check.
     */
    int getItemAmount(Item item);

    /**
     * Set the amount of a dropped item.
     * @param item The item to change the amount to.
     * @param amount The amount to set.
     */
    void setItemAmount(Item item, int amount);

    /**
     * Drop an item on ground.
     * @param location The location to drop the item at.
     * @param itemStack The item to drop.
     * @param amount The amount to drop of this item.
     * @return True if the item was dropped, otherwise false.
     */
    default boolean dropItem(Location location, ItemStack itemStack, int amount){
        return false;
    }

}
