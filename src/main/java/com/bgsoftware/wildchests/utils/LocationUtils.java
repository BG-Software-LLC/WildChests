package com.bgsoftware.wildchests.utils;

import org.bukkit.Location;

public final class LocationUtils {

    public static boolean isInRange(Location loc1, Location loc2, int range){
        return Math.abs(loc1.getBlockX() - loc2.getBlockX()) <= range &&
                Math.abs(loc1.getBlockY() - loc2.getBlockY()) <= range &&
                Math.abs(loc1.getBlockZ() - loc2.getBlockZ()) <= range;
    }

    public static boolean isSameChunk(Location loc1, Location loc2){
        return loc1.getBlockX() >> 4 == loc2.getBlockX() >> 4 && loc1.getBlockZ() >> 4 == loc2.getBlockZ() >> 4;
    }

}
