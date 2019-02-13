package xyz.wildseries.wildchests.api.objects.chests;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import xyz.wildseries.wildchests.api.objects.ChestType;
import xyz.wildseries.wildchests.api.objects.data.ChestData;

import java.util.UUID;

@SuppressWarnings("unused")
public interface Chest {

    UUID getPlacer();

    Location getLocation();

    ChestData getData();

    ChestType getChestType();

    Inventory getPage(int page);

    Inventory[] getPages();

    void setPage(int page, Inventory inventory);

    void openPage(Player player, int page);

    void closePage(Player player);

    int getPagesAmount();

    int getPageIndex(Inventory inventory);

    void remove();

    boolean onBreak(BlockBreakEvent event);

    boolean onPlace(BlockPlaceEvent event);

    boolean onOpen(PlayerInteractEvent event);

    boolean onClose(InventoryCloseEvent event);

    boolean onInteract(InventoryClickEvent event);

    boolean onHopperMove(InventoryMoveItemEvent event);

    boolean onHopperItemTake(Inventory hopperInventory);

}
