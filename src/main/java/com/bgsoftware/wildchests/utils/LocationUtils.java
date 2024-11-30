package com.bgsoftware.wildchests.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import javax.annotation.Nullable;

public final class LocationUtils {

    public static boolean isInRange(Location loc1, Location loc2, int range) {
        return Math.abs(loc1.getBlockX() - loc2.getBlockX()) <= range &&
                Math.abs(loc1.getBlockY() - loc2.getBlockY()) <= range &&
                Math.abs(loc1.getBlockZ() - loc2.getBlockZ()) <= range;
    }

    public static boolean isSameChunk(Location loc1, Location loc2) {
        return loc1.getBlockX() >> 4 == loc2.getBlockX() >> 4 && loc1.getBlockZ() >> 4 == loc2.getBlockZ() >> 4;
    }

    @Nullable
    public static Location fromString(@Nullable String str, boolean allowInvalidLocations) {
        if (str == null || str.isEmpty()) {
            if (allowInvalidLocations)
                return null;

            throw new IllegalArgumentException("Couldn't convert string '" + str + "' into a location.");
        }

        String[] sections = str.split(", ");

        if (sections.length == 4) {
            return new Location(
                    Bukkit.getWorld(sections[0]),
                    Integer.parseInt(sections[1]),
                    Integer.parseInt(sections[2]),
                    Integer.parseInt(sections[3])
            );
        }

        throw new IllegalArgumentException("Couldn't convert string '" + str + "' into a location.");
    }

    public static String toString(Location location) {
        return location.getWorld().getName() + ", " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

}
