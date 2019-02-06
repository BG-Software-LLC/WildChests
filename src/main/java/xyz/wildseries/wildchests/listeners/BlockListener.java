package xyz.wildseries.wildchests.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import xyz.wildseries.wildchests.Locale;
import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.api.objects.chests.Chest;
import xyz.wildseries.wildchests.api.objects.data.ChestData;

@SuppressWarnings("unused")
public final class BlockListener implements Listener {

    private final WildChestsPlugin plugin;
    private final BlockFace[] blockFaces = new BlockFace[]{BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH};

    public BlockListener(WildChestsPlugin plugin){
        this.plugin = plugin;
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

        Chest chest = plugin.getChestsManager().addChest(e.getPlayer().getUniqueId(), e.getBlockPlaced().getLocation(), chestData);

        chest.onPlace(e);

        Locale.CHEST_PLACED.send(e.getPlayer(), chestData.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestBreak(BlockBreakEvent e){
        Chest chest = plugin.getChestsManager().getChest(e.getBlock().getLocation());

        if (chest == null)
            return;

        e.setCancelled(true);

        if(e.getPlayer().getGameMode() != GameMode.CREATIVE)
            e.getPlayer().getWorld().dropItemNaturally(chest.getLocation(), chest.getData().getItemStack());

        chest.onBreak(e);

        chest.remove();
        e.getBlock().setType(Material.AIR);
    }

}
