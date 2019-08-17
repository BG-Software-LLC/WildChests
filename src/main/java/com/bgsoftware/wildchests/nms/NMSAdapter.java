package com.bgsoftware.wildchests.nms;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.key.KeySet;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.stream.Stream;

public interface NMSAdapter {

    String getVersion();

    void playChestAction(Location location, boolean open);

    int getHopperTransfer(World world);

    int getHopperAmount(World world);

    void refreshHopperInventory(Player player, Inventory inventory);

    void setDesignItem(ItemStack itemStack);

    void setTitle(Inventory bukkitInventory, String title);

    String serialize(ItemStack itemStack);

    String serialize(Inventory[] inventories);

    Inventory[] deserialze(String serialized);

    ItemStack deserialzeItem(String serialized);

    void updateTileEntity(Chest chest);

    Stream<Item> getNearbyItems(Location location, int range, boolean onlyChunk, KeySet blacklisted, KeySet whitelisted);

}
