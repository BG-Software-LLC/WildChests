package xyz.wildseries.wildchests.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.api.objects.ChestType;
import xyz.wildseries.wildchests.api.objects.chests.Chest;
import xyz.wildseries.wildchests.api.objects.chests.LinkedChest;
import xyz.wildseries.wildchests.api.objects.data.ChestData;

@SuppressWarnings("unused")
public final class BlockListener implements Listener {

    private final WildChestsPlugin plugin;
    private final BlockFace[] blockFaces = new BlockFace[]{BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH};

    public BlockListener(WildChestsPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void interact(PlayerInteractEvent e){
        if(e.getItem() != null && e.getItem().getType() == Material.LEATHER && e.getAction() == Action.LEFT_CLICK_BLOCK){
            if(e.getClickedBlock().getType() == Material.CHEST){
                e.setCancelled(true);
                ((InventoryHolder) e.getClickedBlock().getState()).getInventory().addItem(new ItemStack(Material.DIRT));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestPlace(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.CHEST && e.getBlockPlaced().getType() != Material.TRAPPED_CHEST)
            return;

        for(BlockFace blockFace : blockFaces){
            Block block = e.getBlockPlaced().getRelative(blockFace);
            if (plugin.getChestsManager().getChest(block.getLocation()) != null) {
                e.setCancelled(true);
                return;
            }
        }

        ChestData chestData = plugin.getChestsManager().getChestData(e.getItemInHand());

        if(chestData == null)
            return;

        plugin.getChestsManager().addChest(e.getPlayer().getUniqueId(), e.getBlockPlaced().getLocation(), chestData);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestBreak(BlockBreakEvent e){
        Chest chest = plugin.getChestsManager().getChest(e.getBlock().getLocation());

        if (chest == null)
            return;

        e.setCancelled(true);

        if(e.getPlayer().getGameMode() != GameMode.CREATIVE)
            e.getPlayer().getWorld().dropItemNaturally(chest.getLocation(), chest.getData().getItemStack());

        if(chest.getChestType() != ChestType.LINKED_CHEST || !((LinkedChest) chest).isLinkedIntoChest()){
            for(int page = 0; page < chest.getPagesAmount(); page++){
                Inventory inventory = chest.getPage(page);
                for(ItemStack itemStack : inventory.getContents())
                    if (itemStack != null && itemStack.getType() != Material.AIR)
                        e.getPlayer().getWorld().dropItemNaturally(chest.getLocation(), itemStack);
                inventory.clear();
            }
        }

        chest.remove();
        e.getBlock().setType(Material.AIR);
    }

}
