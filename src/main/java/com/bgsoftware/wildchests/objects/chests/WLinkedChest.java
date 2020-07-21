package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.StatementHolder;
import com.bgsoftware.wildchests.utils.Executor;
import com.bgsoftware.wildchests.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
public final class WLinkedChest extends WRegularChest implements LinkedChest {

    private Location linkedChest;

    public WLinkedChest(UUID placer, Location location, ChestData chestData){
        super(placer, location, chestData);
        this.linkedChest = null;
    }

    @Override
    public void linkIntoChest(LinkedChest linkedChest) {
        if(linkedChest == null) {
            this.linkedChest = null;
        }

        else if(!linkedChest.isLinkedIntoChest()){
            onBreak(new BlockBreakEvent(null, null));
            this.tileEntityContainer.setTransaction(((WLinkedChest) linkedChest).tileEntityContainer.getTransaction());
            this.inventories = ((WLinkedChest) linkedChest).inventories;
            this.linkedChest = linkedChest.getLocation();
        }

        else{
            linkIntoChest(linkedChest.getLinkedChest());
        }

        LinkedChest _linkedChest = getLinkedChest();
        Query.LINKED_CHEST_UPDATE_TARGET.getStatementHolder()
                .setLocation(linkedChest == null ? null : _linkedChest.getLocation())
                .setLocation(getLocation())
                .execute(true);
    }

    @Override
    public LinkedChest getLinkedChest() {
        if(this.linkedChest == null)
            return null;

        LinkedChest linkedChest = plugin.getChestsManager().getLinkedChest(this.linkedChest);

        if(linkedChest != null && linkedChest.isLinkedIntoChest())
            linkIntoChest(linkedChest);

        return linkedChest;
    }

    @Override
    public boolean isLinkedIntoChest() {
        return getLinkedChest() != null;
    }

    @Override
    public List<LinkedChest> getAllLinkedChests() {
        return plugin.getChestsManager().getAllLinkedChests(this);
    }

    @Override
    public Inventory setPage(int page, int size, String title) {
        if(linkedChest != null){
            return getLinkedChest().setPage(page, size, title);
        }else {
            return super.setPage(page, size, title);
        }
    }

    @Override
    public Inventory getPage(int index) {
        return !isLinkedIntoChest() ? super.getPage(index) : getLinkedChest().getPage(index);
    }

    @Override
    public int getPagesAmount() {
        return !isLinkedIntoChest() ? super.getPagesAmount() : getLinkedChest().getPagesAmount();
    }

    @Override
    public void remove() {
        super.remove();
        //We want to unlink all linked chests only if that's the original chest
        if(this.linkedChest == null)
            getAllLinkedChests().forEach(linkedChest -> linkedChest.linkIntoChest(null));
        //Removing the linked chest from database
        Query.LINKED_CHEST_DELETE.getStatementHolder()
                .setLocation(getLocation())
                .execute(true);
    }

    @Override
    public boolean onBreak(BlockBreakEvent event) {
        if(!isLinkedIntoChest()){
            return super.onBreak(event);
        }

        return true;
    }

    @Override
    public boolean onOpen(PlayerInteractEvent event) {
        if(super.onOpen(event)) {
            if(tileEntityContainer.getViewingCount() != 0)
                getAllLinkedChests().forEach(linkedChest -> plugin.getNMSAdapter().playChestAction(linkedChest.getLocation(), true));
            return true;
        }
        return false;
    }

    @Override
    public boolean onClose(InventoryCloseEvent event) {
        if(super.onClose(event)) {
            if(tileEntityContainer.getViewingCount() == 0)
                getAllLinkedChests().forEach(linkedChest -> plugin.getNMSAdapter().playChestAction(linkedChest.getLocation(), false));
            return true;
        }
        return true;
    }

    @Override
    public void loadFromData(ResultSet resultSet) throws SQLException {
        super.loadFromData(resultSet);
        String linkedChest = resultSet.getString("linked_chest");
        if(!linkedChest.isEmpty()){
            Location linkedChestLocation = LocationUtils.fromString(linkedChest);
            Executor.sync(() -> linkIntoChest(plugin.getChestsManager().getLinkedChest(linkedChestLocation)), 1L);
        }
    }

    @Override
    public void loadFromFile(YamlConfiguration cfg) {
        super.loadFromFile(cfg);
        if (cfg.contains("linked-chest")) {
            //We want to run it on the first tick, after all chests are loaded.
            Location linkedChest = LocationUtils.fromString(cfg.getString("linked-chest"));
            Executor.sync(() -> linkIntoChest(plugin.getChestsManager().getLinkedChest(linkedChest)), 1L);
        }
    }

    @Override
    public void executeInsertQuery(boolean async) {
        Query.LINKED_CHEST_INSERT.getStatementHolder()
                .setLocation(location)
                .setString(placer.toString())
                .setString(getData().getName())
                .setString("")
                .setLocation(linkedChest)
                .execute(async);
    }

    @Override
    public void executeUpdateQuery(boolean async) {
        Query.LINKED_CHEST_UPDATE.getStatementHolder()
                .setString(placer.toString())
                .setString(getData().getName())
                .setInventories(getPages())
                .setLocation(linkedChest)
                .setLocation(location)
                .execute(async);
    }

    @Override
    public StatementHolder getSelectQuery() {
        return Query.LINKED_CHEST_SELECT.getStatementHolder().setLocation(location);
    }

}
