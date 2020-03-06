package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.StatementHolder;
import com.bgsoftware.wildchests.utils.Executor;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.objects.WLocation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
public final class WLinkedChest extends WChest implements LinkedChest {

    private WLocation linkedChest;

    public WLinkedChest(UUID placer, WLocation location, ChestData chestData){
        super(placer, location, chestData);
        this.linkedChest = null;
    }

    @Override
    public void linkIntoChest(LinkedChest linkedChest) {
        if(linkedChest == null) {
            this.linkedChest = null;
            hopperTask.start();
        }else if(!linkedChest.isLinkedIntoChest()){
            this.linkedChest = WLocation.of(linkedChest.getLocation());
            hopperTask.stop();
            if(suctionTask != null)
                suctionTask.stop();
        }else{
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

        LinkedChest linkedChest = plugin.getChestsManager().getLinkedChest(this.linkedChest.getLocation());

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
    public void setPage(int index, Inventory inventory) {
        if(linkedChest != null){
            getLinkedChest().setPage(index, inventory);
        }else {
            super.setPage(index, inventory);
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
        super.onOpen(event);
        getAllLinkedChests().forEach(linkedChest -> plugin.getNMSAdapter().playChestAction(linkedChest.getLocation(), true));
        return true;
    }

    @Override
    public boolean onClose(InventoryCloseEvent event) {
        super.onClose(event);
        getAllLinkedChests().forEach(linkedChest -> plugin.getNMSAdapter().playChestAction(linkedChest.getLocation(), false));
        return true;
    }

    @Override
    public void loadFromData(ResultSet resultSet) throws SQLException {
        super.loadFromData(resultSet);
        String linkedChest = resultSet.getString("linked_chest");
        if(!linkedChest.isEmpty()){
            Location linkedChestLocation = WLocation.of(linkedChest).getLocation();
            Executor.sync(() -> linkIntoChest(plugin.getChestsManager().getLinkedChest(linkedChestLocation)), 1L);
        }
    }

    @Override
    public void loadFromFile(YamlConfiguration cfg) {
        super.loadFromFile(cfg);
        if (cfg.contains("linked-chest")) {
            //We want to run it on the first tick, after all chests are loaded.
            Location linkedChest = WLocation.of(cfg.getString("linked-chest")).getLocation();
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
                .setString("")
                .setLocation(linkedChest)
                .setLocation(location)
                .execute(async);
    }

    @Override
    public StatementHolder getSelectQuery() {
        return Query.LINKED_CHEST_SELECT.getStatementHolder().setLocation(location);
    }

}
