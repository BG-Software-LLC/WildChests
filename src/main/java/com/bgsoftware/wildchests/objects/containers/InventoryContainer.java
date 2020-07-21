package com.bgsoftware.wildchests.objects.containers;

import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public interface InventoryContainer {

    Inventory getPage(int page);

    void openPage(Player player, int page);

    int size();

    Inventory[] getPages();

    Inventory setPage(int page, int size, String title);

    void setPage(int page, InventoryHolder inventory);

    int getPageIndex(Inventory inventory);

    void openContainer(HumanEntity humanEntity);

    void closeContainer(HumanEntity humanEntity);

    int getViewingCount();

}
