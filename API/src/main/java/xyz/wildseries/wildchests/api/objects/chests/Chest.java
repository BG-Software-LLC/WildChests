package xyz.wildseries.wildchests.api.objects.chests;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import xyz.wildseries.wildchests.api.objects.ChestType;
import xyz.wildseries.wildchests.api.objects.data.ChestData;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public interface Chest {

    UUID getPlacer();

    Location getLocation();

    ChestData getData();

    ChestType getChestType();

    Inventory getPage(int page);

    Inventory[] getPages();

    void setPage(int page, Inventory inventory);

    int getPagesAmount();

    int getPageIndex(Inventory inventory);

    void remove();

}
