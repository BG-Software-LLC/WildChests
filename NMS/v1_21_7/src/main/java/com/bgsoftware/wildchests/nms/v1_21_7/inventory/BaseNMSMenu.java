package com.bgsoftware.wildchests.nms.v1_21_7.inventory;

import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.scheduler.Scheduler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.Location;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;

import javax.annotation.Nullable;

public class BaseNMSMenu {

    private final AbstractContainerMenu containerMenu;
    private final Inventory playerInventory;
    private final WildContainer inventory;
    private final Location location;
    @Nullable
    private CraftInventoryView bukkitEntity;

    public BaseNMSMenu(AbstractContainerMenu containerMenu, Inventory playerInventory, WildContainer inventory) {
        this.containerMenu = containerMenu;
        this.playerInventory = playerInventory;
        this.inventory = inventory;
        this.location = inventory.chest.getLocation();
    }

    public CraftInventoryView getBukkitView() {
        if (bukkitEntity == null) {
            CraftWildInventoryImpl inventory = new CraftWildInventoryImpl(this.inventory);
            bukkitEntity = new CraftInventoryView(playerInventory.player.getBukkitEntity(), inventory, containerMenu);
        }

        return bukkitEntity;
    }

    public void removed(Player player) {
        if (Scheduler.isRegionScheduler() && !Scheduler.isScheduledForRegion(location)) {
            Scheduler.runTask(location, () -> doRemoved(player));
        } else {
            doRemoved(player);
        }
    }

    private void doRemoved(Player player) {
        ((WildChestBlockEntity) ((WChest) inventory.chest).getTileEntityContainer()).stopOpen(player);
    }

}
