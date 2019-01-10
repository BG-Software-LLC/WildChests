package xyz.wildseries.wildchests.objects.chests;

import xyz.wildseries.wildchests.api.objects.chests.RegularChest;
import xyz.wildseries.wildchests.api.objects.data.ChestData;
import xyz.wildseries.wildchests.objects.WLocation;

import java.util.UUID;

public final class WRegularChest extends WChest implements RegularChest {

    public WRegularChest(UUID placer, WLocation location, ChestData chestData){
        super(placer, location, chestData);
    }

    @Override
    public void remove() {
        plugin.getChestsManager().removeChest(this);
    }
}
