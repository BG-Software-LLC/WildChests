package com.bgsoftware.wildchests.nms.v1_16_R3.inventory;

import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.server.v1_16_R3.ContainerHopper;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.PlayerInventory;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftInventoryView;

public class WildContainerHopper extends ContainerHopper {

    private final PlayerInventory playerInventory;
    private final WildInventory inventory;
    private CraftInventoryView bukkitEntity;

    private WildContainerHopper(int id, PlayerInventory playerInventory, WildInventory inventory){
        super(id, playerInventory, inventory);
        this.playerInventory = playerInventory;
        this.inventory = inventory;
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if(bukkitEntity == null) {
            CraftWildInventory inventory = new CraftWildInventory(this.inventory);
            bukkitEntity = new CraftInventoryView(playerInventory.player.getBukkitEntity(), inventory, this);
        }

        return bukkitEntity;
    }

    @Override
    public void b(EntityHuman entityhuman) {
        ((TileEntityWildChest) ((WChest) inventory.chest).getTileEntityContainer()).closeContainer(entityhuman);
    }

    public static WildContainerHopper of(int id, PlayerInventory playerInventory, WildInventory inventory){
        return new WildContainerHopper(id, playerInventory, inventory);
    }

}

