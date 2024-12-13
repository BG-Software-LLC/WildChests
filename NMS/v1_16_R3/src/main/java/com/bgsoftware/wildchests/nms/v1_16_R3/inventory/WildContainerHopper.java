package com.bgsoftware.wildchests.nms.v1_16_R3.inventory;

import net.minecraft.server.v1_16_R3.ContainerHopper;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.PlayerInventory;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftInventoryView;

public class WildContainerHopper extends ContainerHopper {

    private final BaseNMSMenu base;

    private WildContainerHopper(int id, PlayerInventory playerInventory, WildInventory inventory) {
        super(id, playerInventory, inventory);
        this.base = new BaseNMSMenu(this, playerInventory, inventory);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        return this.base.getBukkitView();
    }

    @Override
    public void b(EntityHuman entityhuman) {
        this.base.removed(entityhuman);
    }

    public static WildContainerHopper of(int id, PlayerInventory playerInventory, WildInventory inventory) {
        return new WildContainerHopper(id, playerInventory, inventory);
    }

}

