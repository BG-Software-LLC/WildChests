package com.bgsoftware.wildchests.nms.v1_18.inventory;

import com.bgsoftware.wildchests.listeners.InventoryListener;
import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryView;

public class WildChestMenu extends ChestMenu {

    private final Inventory playerInventory;
    private final WildContainer inventory;
    private CraftInventoryView bukkitEntity;

    private WildChestMenu(MenuType<?> menuType, int id, Inventory playerInventory, WildContainer inventory, int rows) {
        super(menuType, id, playerInventory, inventory, rows);
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
        if (!InventoryListener.buyNewPage.containsKey(player.getUUID()))
            ((WildChestBlockEntity) ((WChest) inventory.chest).getTileEntityContainer()).stopOpen(player);
    }

    public static WildChestMenu of(int id, Inventory playerInventory, WildContainer inventory) {
        MenuType<?> menuType;
        int rows;

        switch (inventory.getContainerSize()) {
            case 9 -> {
                rows = 1;
                menuType = MenuType.GENERIC_9x1;
            }
            case 18 -> {
                rows = 2;
                menuType = MenuType.GENERIC_9x2;
            }
            case 27 -> {
                rows = 3;
                menuType = MenuType.GENERIC_9x3;
            }
            case 36 -> {
                rows = 4;
                menuType = MenuType.GENERIC_9x4;
            }
            case 45 -> {
                rows = 5;
                menuType = MenuType.GENERIC_9x5;
            }
            case 54 -> {
                rows = 6;
                menuType = MenuType.GENERIC_9x6;
            }
            default -> {
                throw new IllegalArgumentException("Invalid inventory size: " + inventory.getContainerSize());
            }
        }

        return new WildChestMenu(menuType, id, playerInventory, inventory, rows);
    }

}