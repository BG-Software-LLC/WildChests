package com.bgsoftware.wildchests.api.objects.chests;

import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public interface Chest {

    UUID getPlacer();

    Location getLocation();

    ChestData getData();

    ChestType getChestType();

    Inventory getPage(int page);

    Inventory[] getPages();

    Inventory setPage(int page, int size, String title);

    void openPage(Player player, int page);

    void closePage(Player player);

    int getPagesAmount();

    int getPageIndex(Inventory inventory);

    void remove();

    Map<Integer, ItemStack> addItems(ItemStack... itemStacks);

    void removeItem(int amountToRemove, ItemStack itemStack);

    boolean onBreak(BlockBreakEvent event);

    boolean onOpen(PlayerInteractEvent event);

    boolean onClose(InventoryCloseEvent event);

    boolean onInteract(InventoryClickEvent event);

    /**
     *
     */

    ItemStack[] getContents();

    boolean canTakeItemThroughFace(int slot, ItemStack itemStack);

    int[] getSlotsForFace();

    boolean canPlaceItemThroughFace(ItemStack itemStack);

    ItemStack getItem(int i);

    void setItem(int i, ItemStack itemStack);

    void update();

}
