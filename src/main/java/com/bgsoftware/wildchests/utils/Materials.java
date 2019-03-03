package com.bgsoftware.wildchests.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum Materials {

    BLACK_STAINED_GLASS_PANE("STAINED_GLASS_PANE", 15);


    private static boolean v1_13 = Bukkit.getBukkitVersion().contains("1.13");

    private String legacy;
    private byte data;

    Materials(String legacy, int data){
        this.legacy = legacy;
        this.data = (byte) data;
    }

    public ItemStack toItemStack(int amount){
        return v1_13 ? new ItemStack(Material.valueOf(name()), amount) : new ItemStack(Material.valueOf(legacy), amount, data);
    }

}
