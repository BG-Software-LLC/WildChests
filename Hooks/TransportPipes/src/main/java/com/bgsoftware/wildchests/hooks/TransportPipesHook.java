package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import de.robotricker.transportpipes.api.TransportPipesAPI;
import de.robotricker.transportpipes.api.TransportPipesContainer;
import de.robotricker.transportpipes.duct.pipe.filter.ItemFilter;
import de.robotricker.transportpipes.location.BlockLocation;
import de.robotricker.transportpipes.location.TPDirection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Map;

public final class TransportPipesHook {

    public static void register(WildChestsPlugin plugin) {
        plugin.getProviders().registerChestPlaceListener(TransportPipesHook::placeChest);
        plugin.getProviders().registerChestBreakListener(TransportPipesHook::breakChest);
    }

    private static void placeChest(Chest chest) {
        Location chestLocation = chest.getLocation();
        try {
            TransportPipesAPI.getInstance().registerTransportPipesContainer(
                    new WildChestTransportPipesContainer(chest),
                    new BlockLocation(chestLocation),
                    chestLocation.getWorld());
        } catch (Exception ignored) {

        }
    }

    private static void breakChest(@Nullable OfflinePlayer offlinePlayer, Chest chest) {
        Location chestLocation = chest.getLocation();
        try {
            TransportPipesAPI.getInstance().unregisterTransportPipesContainer(
                    new BlockLocation(chestLocation),
                    chestLocation.getWorld());
        } catch (Exception ignored) {

        }
    }

    private static final class WildChestTransportPipesContainer implements TransportPipesContainer {

        private final Chest chest;

        WildChestTransportPipesContainer(Chest chest) {
            this.chest = chest;
        }

        @Override
        public ItemStack extractItem(TPDirection tpDirection, int amount, ItemFilter itemFilter) {
            if (chest instanceof StorageChest) {
                BigInteger bigAmount = BigInteger.valueOf(amount);
                ItemStack chestItem = ((StorageChest) chest).getItemStack().clone();
                BigInteger chestAmount = ((StorageChest) chest).getAmount();
                if (chestAmount.compareTo(bigAmount) >= 0 && itemFilter.applyFilter(chestItem).getWeight() > 0) {
                    ((StorageChest) chest).setAmount(chestAmount.subtract(bigAmount));
                    chestItem.setAmount(amount);
                    return chestItem;
                }
            } else {
                ItemStack[] chestItems = chest.getContents();
                for (int i = 0; i < chestItems.length; ++i) {
                    ItemStack itemStack = chestItems[i];
                    if (itemStack != null && itemFilter.applyFilter(itemStack).getWeight() > 0) {
                        if (itemStack.getAmount() <= amount) {
                            ItemStack clonedItem = itemStack.clone();
                            chest.setItem(i, new ItemStack(Material.AIR));
                            return clonedItem;
                        } else {
                            ItemStack clonedItem = itemStack.clone();
                            clonedItem.setAmount(amount);
                            itemStack.setAmount(itemStack.getAmount() - amount);
                            return clonedItem;
                        }
                    }
                }
            }

            return null;
        }

        @Override
        public ItemStack insertItem(TPDirection tpDirection, ItemStack itemStack) {
            Map<Integer, ItemStack> additionalItems = chest.addItems(itemStack);
            return additionalItems.isEmpty() ? null : additionalItems.get(0);
        }

        @Override
        public int spaceForItem(TPDirection tpDirection, ItemStack itemStack) {
            return itemStack.getMaxStackSize();
        }

        @Override
        public boolean isInLoadedChunk() {
            return true;
        }

    }

}
