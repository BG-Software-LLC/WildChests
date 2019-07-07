package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.objects.chests.WLinkedChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.bgsoftware.wildchests.api.handlers.ChestsManager;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.chests.RegularChest;
import com.bgsoftware.wildchests.objects.WLocation;
import com.bgsoftware.wildchests.objects.chests.WRegularChest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public final class ChestsHandler implements ChestsManager {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
    private final Set<ChestData> chestsData = new HashSet<>();
    private final Map<WLocation, Chest> chests = new HashMap<>();

    @Override
    public Chest getChest(Location location) {
        return getChest(WLocation.of(location), RegularChest.class);
    }

    @Override
    public LinkedChest getLinkedChest(Location location) {
        return getChest(WLocation.of(location), LinkedChest.class);
    }

    @Override
    public StorageChest getStorageChest(Location location) {
        return getChest(WLocation.of(location), StorageChest.class);
    }

    @Override
    public Chest addChest(UUID placer, Location location, ChestData chestData){
        Chest chest;

        switch (chestData.getChestType()){
            case CHEST:
                chest = new WRegularChest(placer, WLocation.of(location), chestData);
                break;
            case LINKED_CHEST:
                chest = new WLinkedChest(placer, WLocation.of(location), chestData);
                break;
            case STORAGE_UNIT:
                chest = new WStorageChest(placer, WLocation.of(location), chestData);
                break;
            default:
                throw new IllegalArgumentException("Invalid chest at " + location);
        }

        chests.put(WLocation.of(location), chest);
        plugin.getNMSAdapter().updateTileEntity(chest);

        return chest;
    }

    private boolean isChest(Location location) {
        // If the chunk is not loaded, we wil lreturn true without checking the actual block.
        if(!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4))
            return true;

        if(location.getBlock().getType() != Material.CHEST)
            chests.remove(WLocation.of(location));

        return chests.containsKey(WLocation.of(location));
    }

    @Override
    public void removeChest(Chest chest) {
        chests.remove(WLocation.of(chest.getLocation()));
    }

    @Override
    public List<LinkedChest> getAllLinkedChests(LinkedChest linkedChest) {
        List<LinkedChest> linkedChests = new ArrayList<>();

        LinkedChest originalLinkedChest = linkedChest.isLinkedIntoChest() ? linkedChest.getLinkedChest() : linkedChest;

        for (Chest chest : chests.values()) {
            if (chest instanceof LinkedChest) {
                LinkedChest targetChest = (LinkedChest) chest;
                if (targetChest.equals(originalLinkedChest) ||
                        (targetChest.isLinkedIntoChest() && targetChest.getLinkedChest().equals(originalLinkedChest)))
                    linkedChests.add(targetChest);
            }
        }

        return linkedChests;
    }

    @Override
    public ChestData getChestData(String name) {
        for(ChestData chestData : chestsData){
            if(chestData.getName().equalsIgnoreCase(name))
                return chestData;
        }
        return null;
    }

    @Override
    public ChestData getChestData(ItemStack itemStack) {
        for(ChestData chestData : chestsData){
            if(chestData.getItemStack().isSimilar(itemStack))
                return chestData;
        }
        return null;
    }

    @Override
    public List<Chest> getChests() {
        return new ArrayList<>(chests.values());
    }

    @Override
    public List<Chest> getNearbyChests(Location location) {
        return getChests().stream()
                .filter(chest -> {
                    ChestData chestData = chest.getData();
                    if(chestData.isAutoSuctionChunk())
                        return chest.getLocation().getChunk().equals(location.getChunk()) &&
                                Math.abs(location.getBlockY() - chest.getLocation().getBlockY()) <= chest.getData().getAutoSuctionRange();
                    else if(chestData.isAutoSuction()) {
                        return ItemUtils.isInRange(chest.getLocation(), location, chest.getData().getAutoSuctionRange());
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
        return new ArrayList<>(chestsData);
    }

    private <T extends Chest> T getChest(WLocation location, Class<T> chestClass){
        try {
            return isChest(location.getLocation()) ? chestClass.cast(chests.get(location)) : null;
        }catch(ClassCastException ex){
            return null;
        }
    }

}
