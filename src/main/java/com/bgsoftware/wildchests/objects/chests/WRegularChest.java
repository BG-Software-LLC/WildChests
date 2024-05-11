package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.api.objects.chests.RegularChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.api.objects.data.InventoryData;
import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.StatementHolder;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import com.bgsoftware.wildchests.objects.inventory.WildContainerItem;
import com.bgsoftware.wildchests.utils.SyncedArray;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WRegularChest extends WChest implements RegularChest {

    protected SyncedArray<CraftWildInventory> inventories;
    private String serializedData = null;

    public WRegularChest(UUID placer, Location location, ChestData chestData) {
        super(placer, location, chestData);
        initContainer(chestData);
    }

    @Override
    public Inventory getPage(int page) {
        return page < 0 || page >= getPagesAmount() ? null : inventories.get(page);
    }

    @Override
    public Inventory[] getPages() {
        return inventories.stream().toArray(Inventory[]::new);
    }

    @Override
    public Inventory setPage(int page, int size, String title) {
        ChestData chestData = getData();
        checkCapacity(page + 1, chestData.getDefaultSize(), chestData.getDefaultTitle());
        CraftWildInventory inventory = plugin.getNMSInventory().createInventory(this, size, title, page);
        inventories.set(page, inventory);
        updateTitles();
        return inventory;
    }

    @Override
    public int getPagesAmount() {
        return inventories.length();
    }

    @Override
    public int getPageIndex(Inventory inventory) {
        for (int i = 0; i < getPagesAmount(); i++) {
            if (inventories.get(i).equals(inventory))
                return i;
        }

        return -1;
    }

    @Override
    public List<WildContainerItem> getWildContents() {
        List<WildContainerItem> contents = new ArrayList<>();

        int pagesAmount = getPagesAmount();

        for (int page = 0; page < pagesAmount; page++) {
            CraftWildInventory inventory = inventories.get(page);
            contents.addAll(inventory.getWildContents());
        }

        return contents;
    }

    @Override
    public WildContainerItem getWildItem(int i) {
        Inventory firstPage = getPage(0);

        if (firstPage == null)
            return WildContainerItem.AIR.copy();

        int pageSize = firstPage.getSize();
        int page = i / pageSize;
        int slot = i % pageSize;

        CraftWildInventory actualPage = (CraftWildInventory) getPage(page);

        if (actualPage == null)
            return WildContainerItem.AIR.copy();

        return actualPage.getWildItem(slot);
    }

    @Override
    public void setItem(int i, WildContainerItem itemStack) {
        Inventory firstPage = getPage(0);

        if (firstPage == null)
            return;

        int pageSize = firstPage.getSize();
        int page = i / pageSize;
        int slot = i % pageSize;

        CraftWildInventory actualPage = (CraftWildInventory) getPage(page);

        if (actualPage == null)
            return;

        actualPage.setItem(slot, itemStack);
    }

    @Override
    public void onChunkLoad() {
        super.onChunkLoad();
        if (serializedData != null) {
            InventoryHolder[] inventories = plugin.getNMSAdapter().deserialze(serializedData);
            for (int i = 0; i < inventories.length; i++)
                setPage(i, inventories[i]);
            serializedData = null;
        }
    }

    public void loadFromData(String serialized) {
        this.serializedData = !serialized.isEmpty() ? serialized : null;
    }

    @Override
    public StatementHolder setUpdateStatement(StatementHolder statementHolder) {
        return statementHolder.setInventories(getPages()).setLocation(getLocation());
    }

    @Override
    public void executeUpdateStatement(boolean async) {
        setUpdateStatement(Query.REGULAR_CHEST_UPDATE_INVENTORIES.getStatementHolder(this)).execute(async);
    }

    @Override
    public void executeInsertStatement(boolean async) {
        Query.REGULAR_CHEST_INSERT.getStatementHolder(this)
                .setLocation(getLocation())
                .setString(placer.toString())
                .setString(getData().getName())
                .setInventories(getPages())
                .execute(async);
    }

    @Override
    public void executeDeleteStatement(boolean async) {
        Query.REGULAR_CHEST_DELETE.getStatementHolder(this)
                .setLocation(getLocation())
                .execute(async);
    }

    private void checkCapacity(int size, int inventorySize, String inventoryTitle) {
        int oldSize = getPagesAmount();
        if (size > oldSize) {
            inventories.increaseCapacity(size);
            for (int i = oldSize; i < size; i++)
                inventories.set(i, plugin.getNMSInventory().createInventory(this, inventorySize, inventoryTitle, i));
        }
    }

    protected void initContainer(ChestData chestData) {
        this.inventories = new SyncedArray<>(chestData.getDefaultPagesAmount());

        int size = chestData.getDefaultSize();
        Map<Integer, InventoryData> pagesData = chestData.getPagesData();

        for (int i = 0; i < getPagesAmount(); i++) {
            String title = pagesData.containsKey(i + 1) ? pagesData.get(i + 1).getTitle() : chestData.getDefaultTitle();
            inventories.set(i, plugin.getNMSInventory().createInventory(this, size, title, i));
        }
    }

    private void updateTitles() {
        for (int i = 0; i < inventories.length(); i++) {
            inventories.get(i).setTitle(getData().getTitle(i + 1).replace("{0}", getPagesAmount() + ""));
        }
    }

}
