package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.handlers.ChestsManager;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.chests.RegularChest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WLinkedChest;
import com.bgsoftware.wildchests.objects.chests.WRegularChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.objects.data.WChestData;
import com.bgsoftware.wildchests.utils.ChunkPosition;
import com.bgsoftware.wildchests.utils.Executor;
import com.bgsoftware.wildchests.utils.LocationUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public final class ChestsHandler implements ChestsManager {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
    private final Map<String, ChestData> chestsData = new HashMap<>();

    private final Map<Location, Chest> chests = Maps.newConcurrentMap();
    private final Map<ChunkPosition, Set<Chest>> chestsByChunks = Maps.newConcurrentMap();

    @Override
    @Nullable
    public Chest getChest(Location location) {
        return getChest(location, RegularChest.class);
    }

    @Override
    @Nullable
    public LinkedChest getLinkedChest(Location location) {
        return getChest(location, LinkedChest.class);
    }

    @Override
    @Nullable
    public StorageChest getStorageChest(Location location) {
        return getChest(location, StorageChest.class);
    }

    @Override
    public Chest addChest(UUID placer, Location location, ChestData chestData) {
        WChest chest = loadChest(placer, location, chestData);
        plugin.getDataHandler().insertChest(chest);
        Executor.sync(() -> plugin.getNMSInventory().updateTileEntity(chest));
        return chest;
    }

    public WChest loadChest(UUID placer, Location location, ChestData chestData) {
        WChest chest;

        switch (chestData.getChestType()) {
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

        chests.put(location, chest);
        chestsByChunks.computeIfAbsent(ChunkPosition.of(location), s -> Sets.newConcurrentHashSet()).add(chest);

        return chest;
    }

    public void loadChestsData(Map<String, ChestData> chestsData) {
        for (Map.Entry<String, ChestData> entry : chestsData.entrySet()) {
            if (this.chestsData.containsKey(entry.getKey())) {
                ((WChestData) this.chestsData.get(entry.getKey())).loadFromData((WChestData) entry.getValue());
            } else {
                this.chestsData.put(entry.getKey(), entry.getValue());
            }
        }

        chests.values().forEach(chest -> {
            if (((WChest) chest).getTileEntityContainer() != null)
                ((WChest) chest).getTileEntityContainer().updateData();
        });
    }

    @Override
    public void removeChest(Chest chest) {
        chests.remove(chest.getLocation());

        Set<Chest> chunkChests = chestsByChunks.get(ChunkPosition.of(chest.getLocation()));
        if (chunkChests != null)
            chunkChests.remove(chest);

        ((WChest) chest).markAsRemoved();

        plugin.getNMSInventory().removeTileEntity(chest);

        ((WChest) chest).executeDeleteStatement(true);
    }

    @Override
    public List<LinkedChest> getAllLinkedChests(LinkedChest linkedChest) {
        return linkedChest.getAllLinkedChests();
    }

    @Override
    @Nullable
    public ChestData getChestData(String name) {
        return chestsData.get(name.toLowerCase());
    }

    @Override
    @Nullable
    public ChestData getChestData(ItemStack itemStack) {
        String chestName = plugin.getNMSAdapter().getChestName(itemStack);

        if (chestName != null)
            return getChestData(chestName);

        for (ChestData chestData : chestsData.values()) {
            if (((WChestData) chestData).getItemRaw().isSimilar(itemStack))
                return chestData;
        }

        return null;
    }

    @Override
    public List<Chest> getChests() {
        return chests.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(new LinkedList<>(chests.values()));
    }

    @Override
    public List<Chest> getChests(Chunk chunk) {
        Set<Chest> chunkChests = chestsByChunks.get(ChunkPosition.of(chunk));
        return chunkChests == null || chunkChests.isEmpty() ? Collections.emptyList() :
                Collections.unmodifiableList(new LinkedList<>(chunkChests));
    }

    @Override
    public List<Chest> getNearbyChests(Location location) {
        return getChests().stream()
                .filter(chest -> {
                    ChestData chestData = chest.getData();
                    if (chestData.isAutoSuctionChunk()) {
                        return LocationUtils.isSameChunk(chest.getLocation(), location) &&
                                Math.abs(location.getBlockY() - chest.getLocation().getBlockY()) <= chest.getData().getAutoSuctionRange();
                    } else if (chestData.isAutoSuction()) {
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
        return Collections.unmodifiableList(new LinkedList<>(chestsData.values()));
    }

    @Nullable
    private <T extends Chest> T getChest(Location location, Class<T> chestClass) {
        Chest chest = chests.get(location);

        if (chest == null)
            return null;

        if (Bukkit.isPrimaryThread() && location.getBlock().getType() != Material.CHEST) {
            removeChest(chest);
            return null;
        }

        try {
            return chestClass.cast(chest);
        } catch (ClassCastException ex) {
            return null;
        }
    }

}
