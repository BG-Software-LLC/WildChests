package com.bgsoftware.wildchests.api.handlers;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * ChestManager is used to manage all cache of chests.
 */
public interface ChestsManager {

    /**
     * Get a regular chest in a specific location.
     *
     * @param location The location to check.
     * @return The chest object. If no chests were found, or the chest is in an unloaded chunk, null will be returned.
     */
    @Nullable
    Chest getChest(Location location);

    /**
     * Get a linked chest in a specific location.
     *
     * @param location The location to check.
     * @return The chest object. If no chests were found, the chest is not a linked chest,
     * or the chest is in an unloaded chunk, null will be returned.
     */
    @Nullable
    LinkedChest getLinkedChest(Location location);

    /**
     * Get a storage chest in a specific location.
     *
     * @param location The location to check.
     * @return The chest object. If no chests were found, the chest is not a storage chest,
     * or the chest is in an unloaded chunk, null will be returned.
     */
    @Nullable
    StorageChest getStorageChest(Location location);

    /**
     * Add a chest into the database.
     *
     * @param placer    The placer's uuid of the chest.
     * @param location  The location of the chest.
     * @param chestData The settings of the chest.
     * @return The new chest object.
     */
    Chest addChest(UUID placer, Location location, ChestData chestData);

    /**
     * Remove the chest from the database.
     * Do not use this method unless you know what's you are doing. Use Chest#remove instead.
     *
     * @param chest The chest to remove.
     */
    void removeChest(Chest chest);

    /**
     * Get all the chests that are linked into a chest.
     *
     * @param linkedChest The chest to check.
     * @deprecated Moved to LinkedChest#getAllLinkedChests
     */
    @Deprecated
    List<LinkedChest> getAllLinkedChests(LinkedChest linkedChest);

    /**
     * Get the settings object of a chest, by it's name.
     *
     * @param name The name of the chest (similar to config names)
     */
    @Nullable
    ChestData getChestData(String name);

    /**
     * Get the settings object of a chest, by it's item.
     *
     * @param itemStack The item to check (similar to configured item in config)
     */
    @Nullable
    ChestData getChestData(ItemStack itemStack);

    /**
     * Get all the chests on the server, including in unloaded chunks.
     */
    List<Chest> getChests();

    /**
     * Get all the chests in a specific chunk.
     */
    List<Chest> getChests(Chunk chunk);

    /**
     * Get all the nearby chests for a location, for the suction chest.
     *
     * @param location The location to check.
     * @deprecated Not used anymore, can make massive performance spikes.
     */
    @Deprecated
    List<Chest> getNearbyChests(Location location);

    /**
     * Get all the chest settings from config.
     */
    List<ChestData> getAllChestData();

}
