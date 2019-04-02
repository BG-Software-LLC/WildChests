package com.bgsoftware.wildchests.api;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public final class WildChestsAPI {

    private static WildChests instance;

    public static Chest getChest(Location location){
        return instance.getChestsManager().getChest(location);
    }

    public static LinkedChest getLinkedChest(Location location){
        return instance.getChestsManager().getLinkedChest(location);
    }

    public static StorageChest getStorageChest(Location location){
        return instance.getChestsManager().getStorageChest(location);
    }

    public static ChestData getChestData(String name){
        return instance.getChestsManager().getChestData(name);
    }

    public static ChestData getChestData(ItemStack itemStack){
        return instance.getChestsManager().getChestData(itemStack);
    }

    public static WildChests getInstance(){
        return instance;
    }

}
