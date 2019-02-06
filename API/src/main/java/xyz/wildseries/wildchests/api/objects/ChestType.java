package xyz.wildseries.wildchests.api.objects;

import xyz.wildseries.wildchests.api.objects.chests.Chest;
import xyz.wildseries.wildchests.api.objects.chests.LinkedChest;
import xyz.wildseries.wildchests.api.objects.chests.RegularChest;
import xyz.wildseries.wildchests.api.objects.chests.StorageChest;

@SuppressWarnings("unused")
public enum ChestType {

    LINKED_CHEST(LinkedChest.class),
    CHEST(RegularChest.class),
    STORAGE_UNIT(StorageChest.class);

    ChestType(Class<? extends Chest> chestClass){
        this.chestClass = chestClass;
    }

    private Class<? extends Chest> chestClass;

    public Class<? extends Chest> getChestClass(){
        return chestClass;
    }

}
