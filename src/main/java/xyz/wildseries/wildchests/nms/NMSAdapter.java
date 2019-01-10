package xyz.wildseries.wildchests.nms;

import org.bukkit.Location;
import org.bukkit.World;

public interface NMSAdapter {

    String getVersion();

    void playChestAction(Location location, boolean open);

    int getHopperTransfer(World world);

    int getHopperAmount(World world);

}
