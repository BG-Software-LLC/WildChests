package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WLinkedChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.utils.ChunkPosition;
import com.bgsoftware.wildchests.utils.LocationUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.bgsoftware.wildchests.api.handlers.ChestsManager;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.chests.RegularChest;
import com.bgsoftware.wildchests.objects.chests.WRegularChest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public final class ChestsHandler implements ChestsManager {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
    private final Map<String, ChestData> chestsData = new HashMap<>();
    private final Map<Location, WChest> chestsByLocations = Maps.newConcurrentMap();
    private final Map<ChunkPosition, Set<WChest>> chestsByChunks = Maps.newConcurrentMap();

    @Override
    public Chest getChest(Location location) {
        return getChest(location, RegularChest.class);
    }

    @Override
    public LinkedChest getLinkedChest(Location location) {
        return getChest(location, LinkedChest.class);
    }

    @Override
    public StorageChest getStorageChest(Location location) {
        return getChest(location, StorageChest.class);
    }

    @Override
    public Chest addChest(UUID placer, Location location, ChestData chestData){
        WChest chest = loadChest(placer, location, chestData);
        plugin.getDataHandler().insertChest(chest);
        return chest;
    }

    public WChest loadChest(UUID placer, Location location, ChestData chestData){
        WChest chest;

        switch (chestData.getChestType()){
            case CHEST:
                chest = new WRegularChest(placer, location, chestData);
                break;
            case LINKED_CHEST:
                chest = new WLinkedChest(placer, location, chestData);
                break;
            case STORAGE_UNIT:
                chest = new WStorageChest(placer, location, chestData);
                break;
            default:
                throw new IllegalArgumentException("Invalid chest at " + location);
        }

        chestsByLocations.put(location, chest);
        chestsByChunks.computeIfAbsent(ChunkPosition.of(location), s -> Sets.newConcurrentHashSet()).add(chest);
        plugin.getNMSInventory().updateTileEntity(chest);

        return chest;
    }

    public void loadChestsData(Map<String, ChestData> chestsData){
        this.chestsData.clear();
        this.chestsData.putAll(chestsData);
        chestsByLocations.values().forEach(chest -> chest.getTileEntityContainer().updateData());
    }

    private boolean isChest(Location location) {
        // If the chunk is not loaded, we will return true without checking the actual block.
        if(!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4))
            return true;

        if(location.getBlock().getType() != Material.CHEST)
            chestsByLocations.remove(location);

        return chestsByLocations.containsKey(location);
    }

    @Override
    public void removeChest(Chest chest) {
        chestsByLocations.remove(chest.getLocation());
    }

    @Override
    public List<LinkedChest> getAllLinkedChests(LinkedChest linkedChest) {
        return linkedChest.getAllLinkedChests();
    }

    @Override
    public ChestData getChestData(String name) {
        return chestsData.get(name.toLowerCase());
    }

    @Override
    public ChestData getChestData(ItemStack itemStack) {
        for(ChestData chestData : chestsData.values()){
            if(chestData.getItemStack().isSimilar(itemStack))
                return chestData;
        }
        return null;
    }

    @Override
    public List<Chest> getChests() {
        return Collections.unmodifiableList(new ArrayList<>(chestsByLocations.values()));
    }

    @Override
    public List<Chest> getNearbyChests(Location location) {
        return getChests().stream()
                .filter(chest -> {
                    ChestData chestData = chest.getData();
                    if(chestData.isAutoSuctionChunk()) {
                        return LocationUtils.isSameChunk(chest.getLocation(), location) &&
                                Math.abs(location.getBlockY() - chest.getLocation().getBlockY()) <= chest.getData().getAutoSuctionRange();
                    }
                    else if(chestData.isAutoSuction()) {
                        return LocationUtils.isInRange(chest.getLocation(), location, chest.getData().getAutoSuctionRange());
                    }
                    return false;
                }).sorted((c1, c2) -> {
                    double firstDistance = c1.getLocation().distance(location);
                    double secondDistance = c2.getLocation().distance(location);
                    return Double.compare(firstDistance, secondDistance);
                }).collect(Collectors.toList());
    }

    @Override
    public List<ChestData> getAllChestData() {
        return new ArrayList<>(chestsData.values());
    }

    private <T extends Chest> T getChest(Location location, Class<T> chestClass){
        try {
            return isChest(location) ? chestClass.cast(chestsByLocations.get(location)) : null;
        }catch(ClassCastException ex){
            return null;
        }
    }

}
