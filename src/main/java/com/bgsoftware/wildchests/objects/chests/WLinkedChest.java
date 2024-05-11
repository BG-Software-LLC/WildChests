package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.StatementHolder;
import com.bgsoftware.wildchests.objects.containers.LinkedChestsContainer;
import com.bgsoftware.wildchests.utils.Executor;
import com.bgsoftware.wildchests.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class WLinkedChest extends WRegularChest implements LinkedChest {

    private LinkedChestsContainer linkedChestsContainer;

    public WLinkedChest(UUID placer, Location location, ChestData chestData) {
        super(placer, location, chestData);
        this.linkedChestsContainer = null;
    }

    public void setLinkedChestsContainerRaw(@Nullable LinkedChestsContainer linkedChestsContainer) {
        this.linkedChestsContainer = linkedChestsContainer;
        if (linkedChestsContainer == null) {
            initContainer(getData());
        } else {
            this.inventories = linkedChestsContainer.getInventories();
        }
    }

    public void setLinkedChestsContainer(@Nullable LinkedChestsContainer linkedChestsContainer) {
        if (this.linkedChestsContainer == linkedChestsContainer)
            return;

        if (linkedChestsContainer == null) {
            this.linkedChestsContainer.unlinkChest(this);
        } else {
            if (this.linkedChestsContainer != null) {
                linkedChestsContainer.merge(this.linkedChestsContainer);
            } else {
                linkedChestsContainer.linkChest(this);
            }
        }
    }

    @Override
    public void linkIntoChest(@Nullable LinkedChest linkedChest) {
        if (linkedChest == null) {
            setLinkedChestsContainer(null);
            saveLinkedChest();
            return;
        }

        LinkedChestsContainer newContainer = ((WLinkedChest) linkedChest).linkedChestsContainer;
        if (newContainer == null) {
            linkedChest.onBreak(new BlockBreakEvent(null, null));
            newContainer = new LinkedChestsContainer(linkedChest, ((WLinkedChest) linkedChest).inventories);
        }

        setLinkedChestsContainer(newContainer);

        ((WLinkedChest) linkedChest).saveLinkedChest();
        saveLinkedChest();
    }

    public void saveLinkedChest() {
        Query.LINKED_CHEST_UPDATE_LINKED_CHEST.getStatementHolder(this)
                .setLocation(!isLinkedIntoChest() || linkedChestsContainer == null ? null : linkedChestsContainer.getSourceChest().getLocation())
                .setLocation(getLocation())
                .execute(true);
    }

    @Override
    public LinkedChest getLinkedChest() {
        return linkedChestsContainer == null ? null : linkedChestsContainer.getSourceChest();
    }

    @Override
    public boolean isLinkedIntoChest() {
        LinkedChest linkedChest = getLinkedChest();
        return linkedChest != null && linkedChest != this;
    }

    @Override
    public List<LinkedChest> getAllLinkedChests() {
        return linkedChestsContainer == null ? Collections.emptyList() : linkedChestsContainer.getLinkedChests();
    }

    @Override
    public void remove() {
        super.remove();
        //We want to unlink all linked chests only if that's the original chest
        if (linkedChestsContainer != null)
            linkedChestsContainer.unlinkChest(this);
    }

    @Override
    public boolean onBreak(BlockBreakEvent event) {
        if (!isLinkedIntoChest()) {
            return super.onBreak(event);
        }

        return true;
    }

    @Override
    public boolean onOpen(PlayerInteractEvent event) {
        if (super.onOpen(event)) {
            if (tileEntityContainer.getViewingCount() != 0)
                getAllLinkedChests().forEach(linkedChest -> plugin.getNMSAdapter().playChestAction(linkedChest.getLocation(), true));
            return true;
        }

        return false;
    }

    @Override
    public boolean onClose(InventoryCloseEvent event) {
        if (super.onClose(event)) {
            if (tileEntityContainer.getViewingCount() == 0)
                getAllLinkedChests().forEach(linkedChest -> plugin.getNMSAdapter().playChestAction(linkedChest.getLocation(), false));
            return true;
        }

        return true;
    }

    public void loadFromData(String serialized, String linkedChest) {
        super.loadFromData(serialized);
        if (!linkedChest.isEmpty()) {
            Location linkedChestLocation = LocationUtils.fromString(linkedChest);
            Executor.sync(() -> {
                LinkedChest sourceChest = plugin.getChestsManager().getLinkedChest(linkedChestLocation);
                if (sourceChest != null) {
                    if (((WLinkedChest) sourceChest).linkedChestsContainer == null)
                        ((WLinkedChest) sourceChest).linkedChestsContainer = new LinkedChestsContainer(sourceChest,
                                ((WLinkedChest) sourceChest).inventories);

                    this.linkedChestsContainer = ((WLinkedChest) sourceChest).linkedChestsContainer;
                    this.linkedChestsContainer.linkChest(this);

                    this.inventories = ((WLinkedChest) sourceChest).inventories;
                }
            }, 1L);
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
    public StatementHolder setUpdateStatement(StatementHolder statementHolder) {
        return statementHolder.setInventories(isLinkedIntoChest() ? null : getPages()).setLocation(getLocation());
    }

    @Override
    public void executeUpdateStatement(boolean async) {
        setUpdateStatement(Query.LINKED_CHEST_UPDATE_INVENTORIES.getStatementHolder(this)).execute(async);
    }

    @Override
    public void executeInsertStatement(boolean async) {
        Query.LINKED_CHEST_INSERT.getStatementHolder(this)
                .setLocation(getLocation())
                .setString(placer.toString())
                .setString(getData().getName())
                .setInventories(isLinkedIntoChest() ? null : getPages())
                .setLocation(linkedChestsContainer == null ? null : linkedChestsContainer.getSourceChest().getLocation())
                .execute(async);
    }

    @Override
    public void executeDeleteStatement(boolean async) {
        Query.LINKED_CHEST_DELETE.getStatementHolder(this)
                .setLocation(getLocation())
                .execute(async);
    }

}
