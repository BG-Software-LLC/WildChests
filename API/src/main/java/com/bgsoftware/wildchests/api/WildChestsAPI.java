package com.bgsoftware.wildchests.api;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public final class WildChestsAPI {

    private static WildChests instance;

    /**
     * Get a regular chest in a specific location.
     *
     * @param location The location to check.
     * @return The chest object. If no chests were found, or the chest is in an unloaded chunk, null will be returned.
     */
    @Nullable
    public static Chest getChest(Location location) {
        return instance.getChestsManager().getChest(location);
    }

    /**
     * Get a linked chest in a specific location.
     *
     * @param location The location to check.
     * @return The chest object. If no chests were found, the chest is not a linked chest,
     * or the chest is in an unloaded chunk, null will be returned.
     */
    @Nullable
    public static LinkedChest getLinkedChest(Location location) {
        return instance.getChestsManager().getLinkedChest(location);
    }

    /**
     * Get a storage chest in a specific location.
     *
     * @param location The location to check.
     * @return The chest object. If no chests were found, the chest is not a storage chest,
     * or the chest is in an unloaded chunk, null will be returned.
     */
    @Nullable
    public static StorageChest getStorageChest(Location location) {
        return instance.getChestsManager().getStorageChest(location);
    }

    /**
     * Get the settings object of a chest, by it's name.
     *
     * @param name The name of the chest (similar to config names)
     */
    @Nullable
    public static ChestData getChestData(String name) {
        return instance.getChestsManager().getChestData(name);
    }

    /**
     * Get the settings object of a chest, by it's item.
     *
     * @param itemStack The item to check (similar to configured item in config)
     */
    @Nullable
    public static ChestData getChestData(ItemStack itemStack) {
        return instance.getChestsManager().getChestData(itemStack);
    }

    /**
     * Get the instance of the plugin.
     */
    public static WildChests getInstance() {
        return instance;
    }

}
