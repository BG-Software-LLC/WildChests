package xyz.wildseries.wildchests.objects.chests;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.api.objects.ChestType;
import xyz.wildseries.wildchests.api.objects.chests.Chest;
import xyz.wildseries.wildchests.api.objects.data.ChestData;
import xyz.wildseries.wildchests.api.objects.data.InventoryData;
import xyz.wildseries.wildchests.objects.WInventory;
import xyz.wildseries.wildchests.objects.WLocation;
import xyz.wildseries.wildchests.task.ChestTask;
import xyz.wildseries.wildchests.task.HopperTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("all")
public abstract class WChest implements Chest {

    protected final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private final HopperTask hopperTask;
    private final ChestTask chestTask;

    protected final UUID placer;
    protected final WLocation location;
    protected final String name;
    protected final List<WInventory> pages;

    public WChest(UUID placer, WLocation location, ChestData chestData) {
        this.hopperTask = new HopperTask(location);
        this.chestTask = new ChestTask(location);
        this.placer = placer;
        this.location = location;
        this.name = chestData.getName();
        this.pages = new ArrayList<>();
        Map<Integer, InventoryData> pagesData = chestData.getPagesData();
        int size = chestData.getDefaultSize();
        for(int i = 0; i < chestData.getDefaultPagesAmount(); i++){
            String title = pagesData.containsKey(i + 1) ? pagesData.get(i + 1).getTitle() : chestData.getDefaultTitle();
            this.pages.add(i, WInventory.of(size, title));
        }
    }

    @Override
    public UUID getPlacer() {
        return placer;
    }

    @Override
    public Location getLocation() {
        return location.getLocation();
    }

    @Override
    public ChestData getData() {
        return plugin.getChestsManager().getChestData(name);
    }

    @Override
    public ChestType getChestType() {
        return getData().getChestType();
    }

    @Override
    public Inventory getPage(int page) {
        WInventory pageInv = page < 0 || page >= pages.size() ? null : pages.get(page);
        pageInv.setTitle(getData().getTitle(page + 1).replace("{0}", pages.size() + ""));
        return pageInv.getInventory();
    }

    @Override
    public Inventory[] getPages() {
        List<Inventory> inventories = new ArrayList<>();
        pages.forEach(inventory -> inventories.add(inventory.getInventory()));
        return inventories.toArray(new Inventory[0]);
    }

    @Override
    public void setPage(int page, Inventory inventory) {
        ChestData chestData = getData();
        while(page >= pages.size()){
            pages.add(WInventory.of(chestData.getDefaultSize(), chestData.getDefaultTitle()));
        }
        pages.set(page, WInventory.of(inventory));
    }

    @Override
    public int getPagesAmount() {
        return pages.size();
    }

    @Override
    public int getPageIndex(Inventory inventory) {
        for(int i = 0; i < pages.size(); i++){
            if(pages.get(i).getInventory().equals(inventory))
                return i;
        }

        return 0;
    }
}
