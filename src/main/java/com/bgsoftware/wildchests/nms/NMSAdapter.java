package com.bgsoftware.wildchests.nms;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface NMSAdapter {

    String getVersion();

    void playChestAction(Location location, boolean open);

    int getHopperTransfer(World world);

    int getHopperAmount(World world);

    void refreshHopperInventory(Player player, Inventory inventory);

    void setDesignItem(ItemStack itemStack);

    void setTitle(Inventory bukkitInventory, String title);

    default String serialize(ItemStack itemStack){
        return "";
    }

    default String serialize(Inventory[] inventories){
        return "";
    }

    default Inventory[] deserialze(String serialized){
        return new Inventory[0];
    }

    default ItemStack deserialzeItem(String serialized){
        return null;
    }

}
