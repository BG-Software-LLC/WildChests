package com.bgsoftware.wildchests.nms;

import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.key.KeySet;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.stream.Stream;

public interface NMSAdapter {

    int getHopperTransfer(World world);

    int getHopperAmount(World world);

    String serialize(ItemStack itemStack);

    String serialize(Inventory[] inventories);

    InventoryHolder[] deserialze(String serialized);

    ItemStack deserialzeItem(String serialized);

    Stream<Item> getNearbyItems(Location location, int range, boolean onlyChunk, KeySet blacklisted, KeySet whitelisted);

    void spawnSuctionParticle(Location location);

    ItemStack setChestNBT(ItemStack itemStack, ChestType chestType);

    void playChestAction(Location location, boolean open);

}
