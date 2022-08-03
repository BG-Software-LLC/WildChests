package com.bgsoftware.wildchests.nms.v1_19_R1.inventory;

import com.bgsoftware.wildchests.nms.mapping.Remap;
import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ContainerHopper;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftInventoryView;

public class WildContainerHopper extends ContainerHopper {

    private final PlayerInventory playerInventory;
    private final WildInventory inventory;
    private CraftInventoryView bukkitEntity;

    private WildContainerHopper(int id, PlayerInventory playerInventory, WildInventory inventory) {
        super(id, playerInventory, inventory);
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

    @Override
    public void b(EntityHuman entityhuman) {
        ((TileEntityWildChest) ((WChest) inventory.chest).getTileEntityContainer()).closeContainer(entityhuman);
    }

    public static WildContainerHopper of(int id, PlayerInventory playerInventory, WildInventory inventory) {
        return new WildContainerHopper(id, playerInventory, inventory);
    }

}
