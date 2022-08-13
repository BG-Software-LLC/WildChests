package com.bgsoftware.wildchests.nms.v1_18_R2.inventory;

import com.bgsoftware.wildchests.listeners.InventoryListener;
import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.Containers;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryView;

public class WildContainerChest extends ContainerChest {

    private final PlayerInventory playerInventory;
    private final WildInventory inventory;
    private CraftInventoryView bukkitEntity;

    private WildContainerChest(Containers<?> containers, int id, PlayerInventory playerInventory, WildInventory inventory, int rows) {
        super(containers, id, playerInventory, inventory, rows);
        this.playerInventory = playerInventory;
        this.inventory = inventory;
    }

    @Remap(classPath = "net.minecraft.world.entity.player.Inventory",
            name = "player",
            type = Remap.Type.FIELD,
            remappedName = "l")
    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity == null) {
            CraftWildInventory inventory = new CraftWildInventory(this.inventory);
            bukkitEntity = new CraftInventoryView(playerInventory.l.getBukkitEntity(), inventory, this);
        }

        return bukkitEntity;
    }

    @Remap(classPath = "net.minecraft.world.entity.Entity",
            name = "getUUID",
            type = Remap.Type.METHOD,
            remappedName = "cm")
    @Override
    public void b(EntityHuman entityHuman) {
        if (!InventoryListener.buyNewPage.containsKey(entityHuman.cm()))
            ((TileEntityWildChest) ((WChest) inventory.chest).getTileEntityContainer()).closeContainer(entityHuman);
    }

    @Remap(classPath = "net.minecraft.world.inventory.MenuType", name = "GENERIC_9x1", type = Remap.Type.FIELD, remappedName = "a")
    @Remap(classPath = "net.minecraft.world.inventory.MenuType", name = "GENERIC_9x2", type = Remap.Type.FIELD, remappedName = "b")
    @Remap(classPath = "net.minecraft.world.inventory.MenuType", name = "GENERIC_9x3", type = Remap.Type.FIELD, remappedName = "c")
    @Remap(classPath = "net.minecraft.world.inventory.MenuType", name = "GENERIC_9x4", type = Remap.Type.FIELD, remappedName = "d")
    @Remap(classPath = "net.minecraft.world.inventory.MenuType", name = "GENERIC_9x5", type = Remap.Type.FIELD, remappedName = "e")
    @Remap(classPath = "net.minecraft.world.inventory.MenuType", name = "GENERIC_9x6", type = Remap.Type.FIELD, remappedName = "f")
    public static Container of(int id, PlayerInventory playerInventory, WildInventory inventory) {
        Containers<?> containers = Containers.c;
        int rows = 3;

        switch (inventory.b()) {
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