package xyz.wildseries.wildchests.task;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.api.objects.chests.Chest;
import xyz.wildseries.wildchests.api.objects.data.ChestData;
import xyz.wildseries.wildchests.objects.WLocation;
import xyz.wildseries.wildchests.objects.data.WChestData;

import java.util.HashMap;

public final class HopperTask extends BukkitRunnable {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private WLocation location;

    public HopperTask(WLocation location){
        this.location = location;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int hopperTransfer = plugin.getNMSAdapter().getHopperTransfer(location.getWorld());
            runTaskTimerAsynchronously(plugin, hopperTransfer, hopperTransfer);
        }, 1L);
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

        Inventory hopperInventory = getHopperInventory(location.getLocation().getBlock().getRelative(BlockFace.DOWN));

        if(hopperInventory == null)
            return;

        ChestData chestData = chest.getData();

        int hopperAmount = plugin.getNMSAdapter().getHopperAmount(location.getWorld());

        outerLoop: for(int i = 0; i < chest.getPagesAmount(); i++){
            Inventory inventory = chest.getPage(i);
            for(int slot = 0; slot < inventory.getSize(); slot++){
                ItemStack itemStack = inventory.getItem(slot);

                if(itemStack == null || (chestData.isHopperFilter() && !((WChestData) chestData).getRecipesSet().contains(itemStack)))
                    continue;

                int amount = Math.min(getSpaceLeft(hopperInventory, itemStack), hopperAmount);

                if(amount == 0)
                    continue;

                amount = Math.min(amount, itemStack.getAmount());

                ItemStack copyItem = itemStack.clone();

                copyItem.setAmount(amount);

                HashMap<Integer, ItemStack> additionalItems = hopperInventory.addItem(copyItem);

                if(additionalItems.isEmpty()) {
                    if(itemStack.getAmount() > amount){
                        itemStack.setAmount(itemStack.getAmount() - amount);
                    }else{
                        itemStack.setType(Material.AIR);
                    }
                    inventory.setItem(slot, itemStack);
                    break outerLoop;
                }
            }
        }

    }

    private int getSpaceLeft(Inventory inventory, ItemStack itemStack){
        int spaceLeft = 0, counter = 0;

        for(ItemStack _itemStack : inventory.getContents()){
            if(counter >= 5)
                break;
            else if(_itemStack == null || _itemStack.getType() == Material.AIR) {
                spaceLeft += itemStack.getMaxStackSize();
            }
            else if(_itemStack.isSimilar(itemStack)){
                spaceLeft += Math.max(0, _itemStack.getMaxStackSize() - _itemStack.getAmount());
            }
            counter++;
        }
        return spaceLeft;
    }

    private Inventory getHopperInventory(Block hopperBlock){
        if(hopperBlock.getType() == Material.HOPPER)
            return ((Hopper) hopperBlock.getState()).getInventory();

        try {
            for (Entity entity : hopperBlock.getLocation().getChunk().getEntities()) {
                if (entity instanceof HopperMinecart && entity.getLocation().getBlock().equals(hopperBlock)) {
                    return ((HopperMinecart) entity).getInventory();
                }
            }
        }catch(Exception ignored){}

        return null;
    }

}
