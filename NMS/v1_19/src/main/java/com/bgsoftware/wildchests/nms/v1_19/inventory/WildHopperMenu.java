package com.bgsoftware.wildchests.nms.v1_19.inventory;

import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.HopperMenu;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftInventoryView;

public class WildHopperMenu extends HopperMenu {

    private final Inventory playerInventory;
    private final WildContainer inventory;
    private CraftInventoryView bukkitEntity;

    private WildHopperMenu(int id, Inventory playerInventory, WildContainer inventory) {
        super(id, playerInventory, inventory);
        this.playerInventory = playerInventory;
        this.inventory = inventory;
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity == null) {
            CraftWildInventoryImpl inventory = new CraftWildInventoryImpl(this.inventory);
            bukkitEntity = new CraftInventoryView(playerInventory.player.getBukkitEntity(), inventory, this);
        }

        return bukkitEntity;
    }

    @Override
    public void removed(Player player) {
        ((WildChestBlockEntity) ((WChest) inventory.chest).getTileEntityContainer()).stopOpen(player);
    }

    public static WildHopperMenu of(int id, Inventory playerInventory, WildContainer inventory) {
        return new WildHopperMenu(id, playerInventory, inventory);
    }

}
