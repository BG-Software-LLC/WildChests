package com.bgsoftware.wildchests.nms;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;

public interface NMSTileEntity {

    CraftWildInventory createInventory(Chest chest, InventoryHolder inventoryHolder, int page);

}
