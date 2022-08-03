package com.bgsoftware.wildchests.nms.v1_17_R1.inventory;

import com.bgsoftware.wildchests.listeners.InventoryListener;
import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.Containers;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftInventoryView;

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
            bukkitEntity = new CraftInventoryView(playerInventory.l.getBukkitEntity(), inventory, this);
        }

        return bukkitEntity;
    }

    @Override
    public void b(EntityHuman entityhuman) {
        if (!InventoryListener.buyNewPage.containsKey(entityhuman.getUniqueID()))
            ((TileEntityWildChest) ((WChest) inventory.chest).getTileEntityContainer()).closeContainer(entityhuman);
    }

    public static Container of(int id, PlayerInventory playerInventory, WildInventory inventory) {
        Containers<?> containers = Containers.c;
        int rows = 3;

        switch (inventory.getSize()) {
            case 9 -> {
                rows = 1;
                containers = Containers.a;
            }
            case 18 -> {
                rows = 2;
                containers = Containers.b;
            }
            case 36 -> {
                rows = 4;
                containers = Containers.d;
            }
            case 45 -> {
                rows = 5;
                containers = Containers.e;
            }
            case 54 -> {
                rows = 6;
                containers = Containers.f;
            }
        }

        return new WildContainerChest(containers, id, playerInventory, inventory, rows);
    }

}

