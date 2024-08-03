package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class ItemUtils {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    public static void addItem(ItemStack itemStack, Inventory inventory, Location location) {
        HashMap<Integer, ItemStack> additionalItems = inventory.addItem(itemStack);
        if (location != null && !additionalItems.isEmpty()) {
            dropItems(location, additionalItems.values(), false);
        }
    }

    public static String getFormattedType(String type) {
        StringBuilder name = new StringBuilder();
        String[] split = type.split("_");

        for (int i = 0; i < split.length; i++) {
            name.append(split[i].substring(0, 1).toUpperCase()).append(split[i].substring(1).toLowerCase());
            if (i != split.length - 1)
                name.append(" ");
        }

        return name.toString();
    }

    public static int countItems(ItemStack itemStack, Inventory inventory) {
        int amount = 0;

        for (ItemStack _itemStack : inventory.getContents()) {
            if (_itemStack != null && _itemStack.isSimilar(itemStack))
                amount += _itemStack.getAmount();
        }

        return amount;
    }

    public static void dropItems(Location location, Collection<ItemStack> itemStacks, boolean checkForMaxStacks) {
        ItemStackMap<Counter> itemsCount = new ItemStackMap<>();

        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null && itemStack.getType() != Material.AIR)
                itemsCount.computeIfAbsent(itemStack, k -> new Counter()).increase(itemStack.getAmount());
        }

        itemsCount.forEach((itemKey, counter) -> {
            long itemAmount = counter.get();
            while (itemAmount > Integer.MAX_VALUE) {
                itemKey.setAmount(Integer.MAX_VALUE);
                dropItem(location, itemKey, checkForMaxStacks);
                itemAmount -= Integer.MAX_VALUE;
            }
            itemKey.setAmount((int) itemAmount);
            dropItem(location, itemKey, checkForMaxStacks);
        });
    }

    public static void dropItem(Location location, ItemStack itemStack, boolean checkForMaxStacks) {
        if (itemStack.getMaxStackSize() <= 0)
            return;

        if (plugin.getProviders().dropItem(location, itemStack, itemStack.getAmount()))
            return;

        int maxStacks = checkForMaxStacks ? plugin.getSettings().maxStacksOnDrop : -1;
        if (maxStacks == 0)
            return;

        int amountOfFullStacksToDrop = itemStack.getAmount() / itemStack.getMaxStackSize();
        int amountOfStacksToDrop = maxStacks < 0 ? Integer.MAX_VALUE : maxStacks;

        ItemStack cloned = itemStack.clone();

        if (amountOfFullStacksToDrop > 0) {
            cloned.setAmount(itemStack.getMaxStackSize());
            for (int i = 0; i < amountOfFullStacksToDrop && amountOfStacksToDrop > 0; ++i, --amountOfStacksToDrop)
                location.getWorld().dropItemNaturally(location, cloned);
        }

        if (amountOfStacksToDrop > 0 && itemStack.getAmount() % itemStack.getMaxStackSize() > 0) {
            cloned.setAmount(itemStack.getAmount() % itemStack.getMaxStackSize());
            location.getWorld().dropItemNaturally(location, cloned);
        }
    }

    public static void dropOrCollect(Player player, Collection<ItemStack> itemStacks, boolean collect,
                                     Location location, boolean checkForMaxStacks) {
        if (collect && player != null) {
            List<ItemStack> toDropLater = new LinkedList<>();
            for (ItemStack itemStack : itemStacks) {
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    Map<Integer, ItemStack> additionalItems = player.getInventory().addItem(itemStack);
                    toDropLater.addAll(additionalItems.values());
                }
            }

            if (toDropLater.isEmpty())
                return;
            itemStacks = toDropLater;
        }

        dropItems(location, itemStacks, checkForMaxStacks);
    }

    public static void dropOrCollect(Player player, ItemStack itemStack, boolean collect,
                                     Location location, boolean checkForMaxStacks) {
        if (collect && player != null) {
            Map<Integer, ItemStack> additionalItems = player.getInventory().addItem(itemStack);
            if (additionalItems.isEmpty())
                return;
        }

        dropItem(location, itemStack, checkForMaxStacks);
    }

}
