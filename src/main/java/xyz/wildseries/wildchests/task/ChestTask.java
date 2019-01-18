package xyz.wildseries.wildchests.task;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.api.objects.chests.Chest;
import xyz.wildseries.wildchests.api.objects.data.ChestData;
import xyz.wildseries.wildchests.objects.WLocation;
import xyz.wildseries.wildchests.utils.ChestUtils;
import xyz.wildseries.wildchests.utils.ItemUtils;

import java.util.HashSet;
import java.util.Set;

public final class ChestTask extends BukkitRunnable {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private WLocation location;

    public ChestTask(WLocation location){
        this.location = location;
        if(plugin.getSettings().chestTask)
            Bukkit.getScheduler().runTask(plugin, () -> runTaskTimerAsynchronously(plugin, 20L, 20L));
    }

    @Override
    public void run() {
        if(!location.isChunkLoaded())
            return;

        Chest chest = plugin.getChestsManager().getChest(location.getLocation());

        if(chest == null){
            cancel();
            return;
        }

        Block block = chest.getLocation().getBlock();
        Inventory originalInventory;

        if(block.getState() instanceof org.bukkit.block.Chest)
            originalInventory = ((org.bukkit.block.Chest) block.getState()).getBlockInventory();
        else if(block.getState() instanceof org.bukkit.block.DoubleChest)
            originalInventory = ((org.bukkit.block.DoubleChest) block.getState()).getInventory();
        else return;

        Set<ItemStack> additionalItems = new HashSet<>();

        for(int i = 0; i < originalInventory.getSize(); i++){
            ItemStack itemStack = originalInventory.getItem(i);
            if(itemStack != null && itemStack.getType() != Material.AIR){
                if(!ItemUtils.addToChest(chest, itemStack)){
                    additionalItems.add(itemStack);
                }
                originalInventory.setItem(i, new ItemStack(Material.AIR));
            }
        }

        for(ItemStack itemStack : additionalItems){
            originalInventory.addItem(itemStack);
        }

        ChestData chestData = chest.getData();
        if(chestData.isSellMode())
            ChestUtils.trySellChest(chest);
        if(chestData.isAutoCrafter())
            ChestUtils.tryCraftChest(chest);
    }

}
