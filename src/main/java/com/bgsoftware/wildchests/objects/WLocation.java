package com.bgsoftware.wildchests.objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class WLocation {

    private final int x, y, z;
    private final String world;

    private WLocation(String world, int x, int y, int z){
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
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

    public World getWorld(){
        return Bukkit.getWorld(world);
    }

    public Location getLocation(){
        return new Location(Bukkit.getWorld(world), x, y, z);
    }

    public boolean isChunkLoaded(){
        return getWorld().isChunkLoaded(x >> 4, z >> 4);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + (getWorld() != null ? getWorld().hashCode() : 0);
        hash = 19 * hash + (int)(Double.doubleToLongBits(this.x) ^ Double.doubleToLongBits(this.x) >>> 32);
        hash = 19 * hash + (int)(Double.doubleToLongBits(this.y) ^ Double.doubleToLongBits(this.y) >>> 32);
        hash = 19 * hash + (int)(Double.doubleToLongBits(this.z) ^ Double.doubleToLongBits(this.z) >>> 32);
        hash = 19 * hash + Float.floatToIntBits(0);
        hash = 19 * hash + Float.floatToIntBits(0);
        return hash;
    }

    @Override
    public String toString() {
        return world + ", " + x + ", " + y + ", " + z;
    }

    @Override
    public boolean equals(Object obj) {
        Location location = null;

        if(obj instanceof Location)
            location = (Location) obj;
        else if(obj instanceof WLocation)
            location = ((WLocation) obj).getLocation();

        return getLocation().equals(location);
    }

    public static WLocation of(String string){
        if(string.split(", ").length == 4){
            String sections[] = string.split(", ");
            return of(new Location(Bukkit.getWorld(sections[0]), Integer.valueOf(sections[1]), Integer.valueOf(sections[2]), Integer.valueOf(sections[3])));
        }
        throw new IllegalArgumentException("Couldn't convert string '" + string + "' into WLocation.");
    }

    public static WLocation of(Location location){
        return new WLocation(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

}
