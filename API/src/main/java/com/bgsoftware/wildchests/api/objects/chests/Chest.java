package com.bgsoftware.wildchests.api.objects.chests;

import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import org.bukkit.Location;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

/**
 * Basic interface for all the chests.
 */
public interface Chest {

    /* CHEST RELATED METHODS */

    /**
     * Get the uuid of the player that placed the chest.
     */
    UUID getPlacer();

    /**
     * Get the location of the chest.
     */
    Location getLocation();

    /**
     * Get the settings object of the chest.
     */
    ChestData getData();

    /**
     * Get the type of the chest.
     */
    ChestType getChestType();

    /**
     * Remove the chest from the database & cache.
     * This will not delete the block itself, or drop the blocks.
     */
    void remove();

    /* INVENTORIES / PAGES RELATED METHODS */

    /**
     * Get all the contents of the chest.
     */
    ItemStack[] getContents();

    /**
     * Add items to the chest.
     * These items will be sold / crafted, depends on the chest's actions.
     * @param itemStacks The items to add
     * @return See Inventory#addItems return value.
     */
    Map<Integer, ItemStack> addItems(ItemStack... itemStacks);

    /**
     * Add items to the chest.
     * These items will be sold / crafted, depends on the chest's actions.
     * @param itemStacks The items to add
     * @return See Inventory#addItems return value.
     * @deprecated use addItems(ItemStack... itemStacks)
     */
    @Deprecated
    Map<Integer, ItemStack> addRawItems(ItemStack... itemStacks);

    /**
     * Remove an item from the chest.
     * @param amountToRemove The amount to remove from the item.
     * @param itemStack The item to remove.
     */
    void removeItem(int amountToRemove, ItemStack itemStack);

    /**
     * Get an item from the chest in a specific slot.
     * @param slot The slot of the item. Can support all the pages using the following formula:
     *          slot = (page-index) * (pages-size) + (page-slot)
     * @return The item in that slot. If non found, an AIR itemstack will be returned.
     */
    ItemStack getItem(int slot);

    /**
     * Set an item in the chest in a specific slot.
     * @param slot The slot of the item. Can support all the pages using the following formula:
     *          slot = (page-index) * (pages-size) + (page-slot)
     * @param itemStack The item to set.
     */
    void setItem(int slot, ItemStack itemStack);

    /**
     * Get a specific page of this chest.
     * @param page The page index to get.
     *             The index should between 0 and getPagesAmount() return value.
     */
    Inventory getPage(int page);

    /**
     * Get all the pages of this chest.
     */
    Inventory[] getPages();

    /**
     * Set a page with a specific inventory.
     * @param page The page's index. Should between 0 and getPagesAmount() return value.
     * @param inventory The inventory to change the contents to.
     * @deprecated You should use setPage(int page, int size, String title), and then change the contents manually.
     */
    @Deprecated
    void setPage(int page, Inventory inventory);

    /**
     * Set a page with a specific size and title.
     * @param page The page's index. Should between 0 and getPagesAmount() return value.
     * @param size The size of the page. Should be between 9 and 54.
     * @param title The title of the page.
     */
    Inventory setPage(int page, int size, String title);

    /**
     * Open a page for a player.
     * @param player The player to open the page for
     * @param page The page to open. Should between 0 and getPagesAmount() return value.
     */
    void openPage(Player player, int page);

    /**
     * Handle the close of a page.
     * This will not close the page itself (use player#closeInventory), but will clear tracking etc.
     * @param player The player to close the page for.
     */
    void closePage(Player player);

    /**
     * Get the amount of pages for this chest.
     */
    int getPagesAmount();

    /**
     * Get the index of a page.
     * @param inventory The page's inventory.
     * @return The page index. If wasn't found, -1 will be returned. StorageUnit will always return 0,
     * as it doesn't support multiple pages.
     */
    int getPageIndex(Inventory inventory);

    /* BLOCK-ACTIONS RELATED METHODS */

    /**
     * Handle the break of the chest.
     * @param event The BlockBreakEvent that was fired.
     * @return True if succeed, otherwise false.
     */
    boolean onBreak(BlockBreakEvent event);

    /**
     * Handle the placement of the chest.
     * @param event The BlockPlaceEvent that was fired.
     * @return True if succeed, otherwise false.
     * @deprecated Not used in v2.0.0 anymore.
     */
    @Deprecated
    boolean onPlace(BlockPlaceEvent event);

    /**
     * Handle opening the chest.
     * @param event The PlayerInteractEvent that was fired.
     * @return True if succeed, otherwise false.
     */
    boolean onOpen(PlayerInteractEvent event);

    /**
     * Handle closing the chest.
     * @param event The InventoryCloseEvent that was fired.
     * @return True if succeed, otherwise false.
     */
    boolean onClose(InventoryCloseEvent event);

    /**
     * Handle item interactions in the chest.
     * @param event The InventoryClickEvent that was fired.
     * @return True if the interaction was captured, otherwise false.
     */
    boolean onInteract(InventoryClickEvent event);

    /**
     * Handle a hopper moving items into the chest.
     * @param event The InventoryMoveItemEvent that was fired.
     * @return True if items were successfully transferred, otherwise false.
     * @deprecated Not used in v2.0.0 anymore.
     */
    @Deprecated
    boolean onHopperMove(InventoryMoveItemEvent event);

    /**
     * Handle a hopper moving items into the chest.
     * @param itemStack The item that was moved into.
     * @param hopper The hopper that added the item.
     * @return True if items were successfully transferred, otherwise false.
     * @deprecated Not used in v2.0.0 anymore.
     */
    @Deprecated
    boolean onHopperMove(ItemStack itemStack, Hopper hopper);

    /**
     * Handle a hopper moving items out of the chest.
     * @param hopperInventory The inventory of the hopper that took the items.
     * @return True if items were successfully transferred, otherwise false.
     * @deprecated Not used in v2.0.0 anymore.
     */
    @Deprecated
    boolean onHopperItemTake(Inventory hopperInventory);

    /**
     * Check if hoppers can take items in a specific slot.
     * @param slot The slot of the item.
     * @param itemStack The item to take.
     * @return True if they can, otherwise false.
     */
    boolean canTakeItemThroughFace(int slot, ItemStack itemStack);

    /**
     * Get a list of slots that will be checked for hopper interactions.
     */
    int[] getSlotsForFace();

    /**
     * Check if hoppers can move items into the chest.
     * @param itemStack The item to add.
     * @return True if they can, otherwise false.
     */
    boolean canPlaceItemThroughFace(ItemStack itemStack);

}
