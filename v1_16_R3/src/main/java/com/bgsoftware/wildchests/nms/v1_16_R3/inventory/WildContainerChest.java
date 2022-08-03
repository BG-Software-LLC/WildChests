package com.bgsoftware.wildchests.nms.v1_16_R3.inventory;

import com.bgsoftware.wildchests.listeners.InventoryListener;
import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.server.v1_16_R3.Container;
import net.minecraft.server.v1_16_R3.ContainerChest;
import net.minecraft.server.v1_16_R3.Containers;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.PlayerInventory;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftInventoryView;

public class WildContainerChest extends ContainerChest {

    private final PlayerInventory playerInventory;
    private final WildInventory inventory;
    private CraftInventoryView bukkitEntity;

    private WildContainerChest(Containers<?> containers, int id, PlayerInventory playerInventory, WildInventory inventory, int rows) {
        super(containers, id, playerInventory, inventory, rows);
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
        if (!InventoryListener.buyNewPage.containsKey(entityhuman.getUniqueID()))
            ((TileEntityWildChest) ((WChest) inventory.chest).getTileEntityContainer()).closeContainer(entityhuman);
    }

    public static Container of(int id, PlayerInventory playerInventory, WildInventory inventory) {
        Containers<?> containers = Containers.GENERIC_9X3;
        int rows = 3;

        switch (inventory.getSize()) {
            case 9:
                rows = 1;
                containers = Containers.GENERIC_9X1;
                break;
            case 18:
                rows = 2;
                containers = Containers.GENERIC_9X2;
                break;
            case 36:
                rows = 4;
                containers = Containers.GENERIC_9X4;
                break;
            case 45:
                rows = 5;
                containers = Containers.GENERIC_9X5;
                break;
            case 54:
                rows = 6;
                containers = Containers.GENERIC_9X6;
                break;
        }

        return new WildContainerChest(containers, id, playerInventory, inventory, rows);
    }

}

