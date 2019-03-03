package com.bgsoftware.wildchests.nms;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public interface NMSAdapter {

    String getVersion();

    void playChestAction(Location location, boolean open);

    int getHopperTransfer(World world);

    int getHopperAmount(World world);

    void refreshHopperInventory(Player player, Inventory inventory);

}
