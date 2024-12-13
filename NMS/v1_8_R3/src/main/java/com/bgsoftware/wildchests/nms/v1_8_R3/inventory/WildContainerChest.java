package com.bgsoftware.wildchests.nms.v1_8_R3.inventory;

import com.bgsoftware.wildchests.listeners.InventoryListener;
import net.minecraft.server.v1_8_R3.Container;
import net.minecraft.server.v1_8_R3.ContainerChest;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.PlayerInventory;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventoryView;

public class WildContainerChest extends ContainerChest {

    private final BaseNMSMenu base;

    private WildContainerChest(PlayerInventory playerInventory, EntityHuman entityHuman, WildInventory inventory) {
        super(playerInventory, inventory, entityHuman);
        this.base = new BaseNMSMenu(this, playerInventory, inventory);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        return this.base.getBukkitView();
    }

    @Override
    public void b(EntityHuman entityhuman) {
        if (!InventoryListener.buyNewPage.containsKey(entityhuman.getUniqueID()))
            this.base.removed(entityhuman);
    }

    public static Container of(PlayerInventory playerInventory, EntityHuman entityHuman, WildInventory inventory) {
        return new WildContainerChest(playerInventory, entityHuman, inventory);
    }

}
