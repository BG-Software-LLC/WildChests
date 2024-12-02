package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.StatementHolder;
import com.bgsoftware.wildchests.handlers.ChestsHandler;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.scheduler.Scheduler;
import com.bgsoftware.wildchests.utils.LocationUtils;
import com.bgsoftware.wildchests.utils.SyncedArray;
import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class WLinkedChest extends WRegularChest implements LinkedChest {

    @Nullable
    private LinkedChestsChain linkedChestsChain;

    public WLinkedChest(UUID placer, Location location, ChestData chestData) {
        super(placer, location, chestData);
        this.linkedChestsChain = null;
    }

    @Override
    public void linkIntoChest(@Nullable LinkedChest linkedChest) {
        linkIntoChest(linkedChest, true);
    }

    private void linkIntoChest(@Nullable LinkedChest linkedChest, boolean saveData) {
        LinkedChestsChain newChain = linkedChest == null ? null : ((WLinkedChest) linkedChest).linkedChestsChain;

        // If both on the same **VALID** chain, do nothing.
        if (newChain == this.linkedChestsChain && this.linkedChestsChain != null)
            return;

        // In any case we want to unlink this chest from the current chain.
        if (this.linkedChestsChain != null)
            this.linkedChestsChain.unlinkChest(this, saveData);

        if (linkedChest == null) {
            // Unlink operation, let's just save to DB and return.
            if (saveData)
                saveLinkedChest();
            return;
        }

        // Let's simulate this chest being broken
        onBreak(new BlockBreakEvent(null, null));

        // In case the linked chest is not part of chain, let's create for it a new chain.
        if (newChain == null) {
            newChain = ((WLinkedChest) linkedChest).newChain(saveData);
        }

        newChain.linkChest(this, saveData);
    }

    private void setLinkedChestsChain(@Nullable LinkedChestsChain linkedChestsChain, boolean saveData) {
        this.linkedChestsChain = linkedChestsChain;
        if (linkedChestsChain == null) {
            initContainer(getData());
        } else {
            this.inventories = linkedChestsChain.inventories;
        }
        if (saveData)
            saveLinkedChest();
    }

    private LinkedChestsChain newChain(boolean saveData) {
        Preconditions.checkState(this.linkedChestsChain == null, "Cannot create new chain while already being in one");
        this.linkedChestsChain = new LinkedChestsChain(this, this.inventories);
        if (saveData)
            saveLinkedChest();
        return this.linkedChestsChain;
    }

    public void saveLinkedChest() {
        Query.LINKED_CHEST_UPDATE_LINKED_CHEST.getStatementHolder(this)
                .setLocation(isLinkedIntoChest() ? this.linkedChestsChain.sourceChest.getLocation() : null)
                .setLocation(getLocation())
                .execute(true);
    }

    @Override
    public LinkedChest getLinkedChest() {
        return this.linkedChestsChain == null ? null : this.linkedChestsChain.sourceChest;
    }

    @Override
    public boolean isLinkedIntoChest() {
        LinkedChest linkedChest = getLinkedChest();
        return linkedChest != null && linkedChest != this;
    }

    @Override
    public List<LinkedChest> getAllLinkedChests() {
        return this.linkedChestsChain == null ? Collections.emptyList() :
                Collections.unmodifiableList(new LinkedList<>(this.linkedChestsChain.linkedChests));
    }

    @Override
    public void remove() {
        super.remove();
        //We want to unlink all linked chests only if that's the original chest
        if (this.linkedChestsChain != null)
            this.linkedChestsChain.unlinkChest(this, true);
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

    @Override
    public void loadFromData(ChestsHandler.UnloadedChest unloadedChest) {
        if (!(unloadedChest instanceof ChestsHandler.UnloadedRegularChest)) {
            WildChestsPlugin.log("&cCannot load data to chest " + getLocation() + " from " + unloadedChest);
            return;
        }

        super.loadFromData(unloadedChest);

        Location linkedChestLocation = ((ChestsHandler.UnloadedRegularChest) unloadedChest).linkedChest;
        if (linkedChestLocation == null)
            return;

        Scheduler.runTask(() -> {
            LinkedChest sourceChest = plugin.getChestsManager().getLinkedChest(linkedChestLocation);
            if (sourceChest != null) {
                linkIntoChest(sourceChest, false);
            }
        }, 1L);
    }

    @Override
    public void loadFromFile(YamlConfiguration cfg) {
        super.loadFromFile(cfg);
        if (cfg.contains("linked-chest")) {
            //We want to run it on the first tick, after all chests are loaded.
            Location linkedChest = LocationUtils.fromString(cfg.getString("linked-chest"), true);
            if (linkedChest != null)
                Scheduler.runTask(linkedChest, () -> linkIntoChest(plugin.getChestsManager().getLinkedChest(linkedChest)), 1L);
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
        boolean isLinkedIntoChest = isLinkedIntoChest();
        Query.LINKED_CHEST_INSERT.getStatementHolder(this)
                .setLocation(getLocation())
                .setString(placer.toString())
                .setString(getData().getName())
                .setInventories(isLinkedIntoChest ? null : getPages())
                .setLocation(isLinkedIntoChest ? this.linkedChestsChain.sourceChest.getLocation() : null)
                .execute(async);
    }

    @Override
    public void executeDeleteStatement(boolean async) {
        Query.LINKED_CHEST_DELETE.getStatementHolder(this)
                .setLocation(getLocation())
                .execute(async);
    }

    private static class LinkedChestsChain {

        private final Set<WLinkedChest> linkedChests = new LinkedHashSet<>();
        private final SyncedArray<CraftWildInventory> inventories;
        private final WLinkedChest sourceChest;

        private boolean destroyed = false;

        LinkedChestsChain(WLinkedChest sourceChest, SyncedArray<CraftWildInventory> inventories) {
            this.inventories = inventories;
            this.sourceChest = sourceChest;
            this.linkedChests.add(sourceChest);
        }

        void linkChest(WLinkedChest linkedChest, boolean saveData) {
            ensureNotDestroyed();
            if (linkedChests.add(linkedChest))
                linkedChest.setLinkedChestsChain(this, saveData);
        }

        void unlinkChest(WLinkedChest linkedChest, boolean saveData) {
            ensureNotDestroyed();
            if (this.sourceChest == linkedChest) {
                destroy(saveData);
            } else if (this.linkedChests.remove(linkedChest)) {
                linkedChest.setLinkedChestsChain(null, saveData);
            }
        }

        void destroy(boolean saveData) {
            ensureNotDestroyed();
            this.linkedChests.forEach(linkedChest ->
                    linkedChest.setLinkedChestsChain(null, saveData));
            this.linkedChests.clear();
            this.destroyed = true;
        }

        private void ensureNotDestroyed() {
            Preconditions.checkState(!this.destroyed, "Used destroyed chain");
        }

    }

}
