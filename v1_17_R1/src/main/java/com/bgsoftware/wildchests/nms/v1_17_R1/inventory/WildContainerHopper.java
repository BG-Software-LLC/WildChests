package com.bgsoftware.wildchests.nms.v1_17_R1.inventory;

import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ContainerHopper;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftInventoryView;

public class WildContainerHopper extends ContainerHopper {

    private final PlayerInventory playerInventory;
    private final WildInventory inventory;
    private CraftInventoryView bukkitEntity;

    private WildContainerHopper(int id, PlayerInventory playerInventory, WildInventory inventory) {
        super(id, playerInventory, inventory);
        this.playerInventory = playerInventory;
        this.inventory = inventory;
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity == null) {
            CraftWildInventory inventory = new CraftWildInventory(this.inventory);
            bukkitEntity = new CraftInventoryView(playerInventory.l.getBukkitEntity(), inventory, this);
        }

        return bukkitEntity;
    }

    @Override
    public void b(EntityHuman entityhuman) {
        ((TileEntityWildChest) ((WChest) inventory.chest).getTileEntityContainer()).closeContainer(entityhuman);
    }

    public static WildContainerHopper of(int id, PlayerInventory playerInventory, WildInventory inventory) {
        return new WildContainerHopper(id, playerInventory, inventory);
    }

}

