package com.bgsoftware.wildchests.listeners;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.task.HopperTask;
import com.bgsoftware.wildchests.utils.ItemUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

        Locale.CHEST_PLACED.send(e.getPlayer(), chestData.getName(), e.getItemInHand().getItemMeta().getDisplayName());

        //Update the hopper below the chest
        Block hopperBlock = e.getBlockPlaced().getRelative(BlockFace.DOWN);
        if(hopperBlock.getState() instanceof Hopper)
            HopperTask.addHopper(chest, (Hopper) hopperBlock.getState());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestBreak(BlockBreakEvent e){
        Chest chest = plugin.getChestsManager().getChest(e.getBlock().getLocation());

        if (chest == null)
            return;

        e.setCancelled(true);

        if(e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            ChestData chestData = chest.getData();
            ItemUtils.dropOrCollect(e.getPlayer(), chestData.getItemStack(), chestData.isAutoCollect(), chest.getLocation());
        }

        chest.onBreak(e);

        chest.remove();
        e.getBlock().setType(Material.AIR);

        //Update the hopper below the chest
        HopperTask.removeHopper(chest);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestExplode(EntityExplodeEvent e){
        List<Block> blockList = new ArrayList<>(e.blockList());
        for(Block block : blockList) {
            Chest chest = plugin.getChestsManager().getChest(block.getLocation());

            if (chest == null)
                continue;

            e.blockList().remove(block);

            if(plugin.getSettings().explodeDropChance > 0 && (plugin.getSettings().explodeDropChance == 100 ||
                    ThreadLocalRandom.current().nextInt(101) <= plugin.getSettings().explodeDropChance)) {
                ChestData chestData = chest.getData();
                ItemUtils.dropOrCollect(null, chestData.getItemStack(), false, chest.getLocation());
            }

            chest.onBreak(new BlockBreakEvent(block, null));

            chest.remove();
            block.setType(Material.AIR);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopperPlace(BlockPlaceEvent e){
        if(e.getBlock().getType() != Material.HOPPER)
            return;

        Chest chest = plugin.getChestsManager().getChest(e.getBlock().getRelative(BlockFace.UP).getLocation());

        if(chest != null)
            HopperTask.addHopper(chest, (Hopper) e.getBlock().getState());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopperBreak(BlockBreakEvent e){
        if(e.getBlock().getType() != Material.HOPPER)
            return;

        Chest chest = plugin.getChestsManager().getChest(e.getBlock().getRelative(BlockFace.UP).getLocation());

        if(chest != null)
            HopperTask.removeHopper(chest);
    }

}
