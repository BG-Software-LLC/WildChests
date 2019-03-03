package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.objects.WLocation;
import com.bgsoftware.wildchests.api.objects.chests.RegularChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;

import java.util.UUID;

public final class WRegularChest extends WChest implements RegularChest {

    public WRegularChest(UUID placer, WLocation location, ChestData chestData){
        super(placer, location, chestData);
    }

}
