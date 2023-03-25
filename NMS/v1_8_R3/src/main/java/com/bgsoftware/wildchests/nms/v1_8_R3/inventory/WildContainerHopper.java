package com.bgsoftware.wildchests.nms.v1_8_R3.inventory;

import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.server.v1_8_R3.ContainerHopper;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.PlayerInventory;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventoryView;

public class WildContainerHopper extends ContainerHopper {

    private final PlayerInventory playerInventory;
    private final WildInventory inventory;
    private CraftInventoryView bukkitEntity;

    private WildContainerHopper(PlayerInventory playerInventory, EntityHuman entityHuman, WildInventory inventory) {
        super(playerInventory, inventory, entityHuman);
        this.playerInventory = playerInventory;
        this.inventory = inventory;
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity == null) {
            CraftWildInventory inventory = new CraftWildInventory(this.inventory);
            bukkitEntity = new CraftInventoryView(playerInventory.player.getBukkitEntity(), inventory, this);
        }

        return bukkitEntity;
    }

    @Override
    public void b(EntityHuman entityhuman) {
        ((TileEntityWildChest) ((WChest) inventory.chest).getTileEntityContainer()).closeContainer(entityhuman);
    }

    public static WildContainerHopper of(PlayerInventory playerInventory, EntityHuman entityHuman, WildInventory inventory) {
        return new WildContainerHopper(playerInventory, entityHuman, inventory);
    }

}

