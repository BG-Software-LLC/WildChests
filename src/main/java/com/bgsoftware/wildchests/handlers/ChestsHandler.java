package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WLinkedChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.objects.data.WChestData;
import com.bgsoftware.wildchests.utils.ChunkPosition;
import com.bgsoftware.wildchests.utils.Executor;
import com.bgsoftware.wildchests.utils.LocationUtils;
import com.google.common.collect.Maps;
import org.bukkit.Chunk;
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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public final class ChestsHandler implements ChestsManager {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
    private final Map<String, ChestData> chestsData = new HashMap<>();

    private final Map<ChunkPosition, Map<Location, Chest>> chests = Maps.newConcurrentMap();

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
        Executor.sync(() -> plugin.getNMSInventory().updateTileEntity(chest));
        return chest;
    }

    public WChest loadChest(UUID placer, Location location, ChestData chestData){
        WChest chest;

        switch (chestData.getChestType() ){
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

        chests.computeIfAbsent(ChunkPosition.of(location), s -> Maps.newConcurrentMap()).put(location, chest);

        return chest;
    }

    public void loadChestsData(Map<String, ChestData> chestsData){
        for(Map.Entry<String, ChestData> entry : chestsData.entrySet()){
            if(this.chestsData.containsKey(entry.getKey())){
                ((WChestData) this.chestsData.get(entry.getKey())).loadFromData((WChestData) entry.getValue());
            }
            else{
                this.chestsData.put(entry.getKey(), entry.getValue());
            }
        }

        chests.values().forEach(map -> map.values().forEach(chest -> {
            if(((WChest) chest).getTileEntityContainer() != null)
                ((WChest) chest).getTileEntityContainer().updateData();
        }));
    }

    @Override
    public void removeChest(Chest chest) {
        Map<Location, Chest> chunkChests = chests.get(ChunkPosition.of(chest.getLocation()));
        if(chunkChests != null)
            chunkChests.remove(chest.getLocation());

        ((WChest) chest).markAsRemoved();

        plugin.getNMSInventory().removeTileEntity(chest);
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
        String chestName = plugin.getNMSAdapter().getChestName(itemStack);

        if(chestName != null)
            return getChestData(chestName);

        for(ChestData chestData : chestsData.values()){
            if(((WChestData) chestData).getItemRaw().isSimilar(itemStack))
                return chestData;
        }

        return null;
    }

    @Override
    public List<Chest> getChests() {
        return chests.values().stream().flatMap((Function<Map<Location, Chest>, Stream<Chest>>) locationChestMap ->
                locationChestMap.values().stream()).collect(Collectors.toList());
    }

    @Override
    public List<Chest> getChests(Chunk chunk) {
        Map<Location, Chest> chunkChests = chests.get(ChunkPosition.of(chunk));
        return Collections.unmodifiableList(chunkChests == null ? new ArrayList<>() : new ArrayList<>(chunkChests.values()));
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
        if(!isChest(location))
            return null;

        Map<Location, Chest> chunkChests = chests.get(ChunkPosition.of(location));

        if(chunkChests == null)
            return null;

        Chest chest = chunkChests.get(location);

        try {
            return chestClass.cast(chest);
        }catch(ClassCastException ex){
            WildChestsPlugin.log("&cTried to cast " + chest.getClass() + " into " + chestClass + ". Stack trace:");
            ex.printStackTrace();
            return null;
        }
    }

    private boolean isChest(Location location) {
        // If the chunk is not loaded, we will return true without checking the actual block.
        if(!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4))
            return true;

        Map<Location, Chest> chunkChests = chests.get(ChunkPosition.of(location));

        if(chunkChests == null)
            return false;

        if(location.getBlock().getType() != Material.CHEST) {
            Chest chest = chunkChests.remove(location);
            if(chest != null)
                removeChest(chest);

            return false;
        }

        return chunkChests.containsKey(location);
    }

}
