package com.bgsoftware.wildchests.nms.v1_17.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.HopperMenu;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftInventoryView;

public class WildHopperMenu extends HopperMenu {

    private final BaseNMSMenu base;

    private WildHopperMenu(int id, Inventory playerInventory, WildContainer inventory) {
        super(id, playerInventory, inventory);
        this.base = new BaseNMSMenu(this, playerInventory, inventory);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        return this.base.getBukkitView();
    }

    @Override
    public void removed(Player player) {
        this.base.removed(player);
    }

    public static WildHopperMenu of(int id, Inventory playerInventory, WildContainer inventory) {
        return new WildHopperMenu(id, playerInventory, inventory);
    }

}
