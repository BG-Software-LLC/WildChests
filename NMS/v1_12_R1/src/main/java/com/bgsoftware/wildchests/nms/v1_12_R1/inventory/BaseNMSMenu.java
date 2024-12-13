package com.bgsoftware.wildchests.nms.v1_12_R1.inventory;

import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.scheduler.Scheduler;
import net.minecraft.server.v1_12_R1.Container;
import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.PlayerInventory;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftInventoryView;

import javax.annotation.Nullable;

public class BaseNMSMenu {

    private final Container container;
    private final PlayerInventory playerInventory;
    private final WildInventory inventory;
    private final Location location;
    @Nullable
    private CraftInventoryView bukkitEntity;

    public BaseNMSMenu(Container container, PlayerInventory playerInventory, WildInventory inventory) {
        this.container = container;
        this.playerInventory = playerInventory;
        this.inventory = inventory;
        this.location = inventory.chest.getLocation();
    }

    public CraftInventoryView getBukkitView() {
        if (bukkitEntity == null) {
            CraftWildInventory inventory = new CraftWildInventory(this.inventory);
            bukkitEntity = new CraftInventoryView(playerInventory.player.getBukkitEntity(), inventory, container);
        }

        return bukkitEntity;
    }

    public void removed(EntityHuman entityHuman) {
        if (Scheduler.isRegionScheduler() && !Scheduler.isScheduledForRegion(location)) {
            Scheduler.runTask(location, () -> doRemoved(entityHuman));
        } else {
            doRemoved(entityHuman);
        }
    }

    private void doRemoved(EntityHuman entityHuman) {
        ((TileEntityWildChest) ((WChest) inventory.chest).getTileEntityContainer()).closeContainer(entityHuman);
    }

}
