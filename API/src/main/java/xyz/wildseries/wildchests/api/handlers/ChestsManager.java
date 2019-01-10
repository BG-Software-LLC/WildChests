package xyz.wildseries.wildchests.api.handlers;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import xyz.wildseries.wildchests.api.objects.chests.Chest;
import xyz.wildseries.wildchests.api.objects.chests.LinkedChest;
import xyz.wildseries.wildchests.api.objects.data.ChestData;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnusedReturnValue")
public interface ChestsManager {

    Chest getChest(Location location);

    LinkedChest getLinkedChest(Location location);

    Chest addChest(UUID placer, Location location, ChestData chestData);

    void removeChest(Chest chest);

    List<LinkedChest> getAllLinkedChests(LinkedChest linkedChest);

    ChestData getChestData(String name);

    ChestData getChestData(ItemStack itemStack);

    List<Chest> getChests();

    List<ChestData> getAllChestData();

}
