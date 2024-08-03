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
import com.bgsoftware.wildchests.scheduler.Scheduler;
import com.bgsoftware.wildchests.utils.BlockPosition;
import com.bgsoftware.wildchests.utils.ChunkPosition;
import com.bgsoftware.wildchests.utils.LocationUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private final Map<BlockPosition, Chest> chests = Maps.newConcurrentMap();
    private final Map<ChunkPosition, Set<Chest>> chestsByChunks = Maps.newConcurrentMap();
    private final Map<String, Set<Chest>> chestsByWorlds = Maps.newConcurrentMap();
    private final Map<ChunkPosition, Map<BlockPosition, UnloadedChest>> unloadedChests = Maps.newConcurrentMap();

    @Override
    @Nullable

    public Chest getChest(Location location) {
        return getChest(BlockPosition.of(location), RegularChest.class);
    }

    @Override
    @Nullable
    public LinkedChest getLinkedChest(Location location) {
        return getChest(BlockPosition.of(location), LinkedChest.class);
    }

    @Override
    @Nullable
    public StorageChest getStorageChest(Location location) {
        return getChest(BlockPosition.of(location), StorageChest.class);
    }

    @Override
    public Chest addChest(UUID placer, Location location, ChestData chestData) {
        WChest chest = createChestInternal(placer, location, chestData);
        plugin.getDataHandler().insertChest(chest);
        Scheduler.runTask(location, () -> plugin.getNMSInventory().updateTileEntity(chest));
        return chest;
    }

    public void loadUnloadedChest(UUID placer, BlockPosition position, ChestData chestData, String[] extendedData) {
        UnloadedChest unloadedChest = new UnloadedChest(placer, position, chestData, extendedData);
        unloadedChests.computeIfAbsent(ChunkPosition.of(position), s -> new LinkedHashMap<>()).put(position, unloadedChest);
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
        chests.remove(BlockPosition.of(chest.getLocation()));

        Set<Chest> chunkChests = chestsByChunks.get(ChunkPosition.of(chest.getLocation()));
        if (chunkChests != null)
            chunkChests.remove(chest);

        Set<Chest> worldChests = chestsByWorlds.get(chest.getLocation().getWorld().getName());
        if (worldChests != null)
            worldChests.remove(chest);

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

    public List<Chest> getChests(World world) {
        Set<Chest> chunkChests = chestsByWorlds.get(world.getName());
        return chunkChests == null || chunkChests.isEmpty() ? Collections.emptyList() :
                Collections.unmodifiableList(new LinkedList<>(chunkChests));
    }

    @Override
    public List<Chest> getNearbyChests(Location location) {
        return getChests(location.getWorld()).stream()
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
    private <T extends Chest> T getChest(BlockPosition blockPosition, Class<T> chestClass) {
        World world = Bukkit.getWorld(blockPosition.getWorldName());

        if (world == null)
            return null;

        Chest chest = chests.get(blockPosition);

        if (chest == null) {
            ChunkPosition chunkPosition = ChunkPosition.of(blockPosition);
            Map<BlockPosition, UnloadedChest> unloadedChests = this.unloadedChests.get(chunkPosition);
            if (unloadedChests == null)
                return null;

            UnloadedChest unloadedChest = unloadedChests.remove(blockPosition);
            if (unloadedChest == null)
                return null;

            if (unloadedChests.isEmpty())
                this.unloadedChests.remove(chunkPosition);

            chest = loadChestInternal(unloadedChest);
        }

        Location location = new Location(world, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());

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

    public void loadChestsForChunk(Chunk chunk) {
        Map<BlockPosition, UnloadedChest> unloadedChests = this.unloadedChests.remove(ChunkPosition.of(chunk));
        if (unloadedChests != null)
            unloadedChests.values().forEach(this::loadChestInternal);
    }

    private WChest createChestInternal(UUID placer, Location location, ChestData chestData) {
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

        chests.put(BlockPosition.of(location), chest);
        chestsByChunks.computeIfAbsent(ChunkPosition.of(location), s -> Sets.newConcurrentHashSet()).add(chest);
        chestsByWorlds.computeIfAbsent(location.getWorld().getName(), s -> Sets.newConcurrentHashSet()).add(chest);

        return chest;
    }

    private WChest loadChestInternal(UnloadedChest unloadedChest) {
        World world = Bukkit.getWorld(unloadedChest.position.getWorldName());

        if (world == null) {
            throw new IllegalArgumentException("Tried to load chest for an invalid world: " +
                    unloadedChest.position.getWorldName());
        }

        Location location = new Location(world, unloadedChest.position.getX(),
                unloadedChest.position.getY(), unloadedChest.position.getZ());

        WChest chest = createChestInternal(unloadedChest.placer, location, unloadedChest.chestData);

        if (chest instanceof StorageChest) {
            String item = unloadedChest.extendedData[0];
            String amount = unloadedChest.extendedData[1];
            String maxAmount = unloadedChest.extendedData[2];
            ((WStorageChest) chest).loadFromData(item, amount, maxAmount);
        } else {
            String serialized = unloadedChest.extendedData[0];
            if (chest instanceof LinkedChest) {
                String linkedChest = unloadedChest.extendedData[1];
                ((WLinkedChest) chest).loadFromData(serialized, linkedChest);
            } else {
                ((WRegularChest) chest).loadFromData(serialized);
            }

            if (!serialized.isEmpty() && serialized.toCharArray()[0] != '*') {
                chest.executeUpdateStatement(true);
            }
        }

        return chest;
    }

    private static class UnloadedChest {

        private final UUID placer;
        private final BlockPosition position;
        private final ChestData chestData;
        private final String[] extendedData;

        UnloadedChest(UUID placer, BlockPosition position, ChestData chestData, String[] extendedData) {
            this.placer = placer;
            this.position = position;
            this.chestData = chestData;
            this.extendedData = extendedData;
        }

    }

}
