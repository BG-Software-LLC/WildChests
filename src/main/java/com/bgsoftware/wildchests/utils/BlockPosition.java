package com.bgsoftware.wildchests.utils;

import org.bukkit.Location;

import java.util.Objects;

public class BlockPosition {

    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

    public static BlockPosition deserialize(String serialized) {
        String[] sections = serialized.split(", ");
        return new BlockPosition(sections[0], Integer.parseInt(sections[1]),
                Integer.parseInt(sections[2]), Integer.parseInt(sections[3]));
    }

    public static BlockPosition of(Location location) {
        return new BlockPosition(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public BlockPosition(String worldName, int x, int y, int z) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockPosition that = (BlockPosition) o;
        return x == that.x && y == that.y && z == that.z && Objects.equals(worldName, that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, y, z);
    }

}
