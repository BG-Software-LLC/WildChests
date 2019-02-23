package com.bgsoftware.wildchests.objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum Materials {

    GREEN_STAINED_GLASS_PANE("STAINED_GLASS_PANE", 13),
    RED_STAINED_GLASS_PANE("STAINED_GLASS_PANE", 14),
    BLACK_STAINED_GLASS_PANE("STAINED_GLASS_PANE", 15);

    Materials(String legacyType, int legacyData){
        this.legacyType = legacyType;
        this.legacyData = (byte) legacyData;
    }

    private String legacyType;
    private byte legacyData;

    private static boolean v1_13 = Bukkit.getBukkitVersion().contains("1.13");

    public ItemStack toBukkitItem(){
        return v1_13 ? new ItemStack(Material.matchMaterial(name())) : new ItemStack(Material.matchMaterial(legacyType), 1, legacyData);
    }

}
