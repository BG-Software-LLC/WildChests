package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.events.SellChestTaskEvent;
import com.bgsoftware.wildchests.api.key.Key;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.handlers.ProvidersHandler;
import com.bgsoftware.wildchests.objects.data.WChestData;
import com.bgsoftware.wildchests.task.NotifierTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

public final class ChestUtils {

    private static final int DEFAULT_MAX_STACK_SIZE = 64;

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
    public static final short DEFAULT_COOLDOWN = 20;

    public static final BiPredicate<Item, ChestData> SUCTION_PREDICATE = (item, chestData) -> {
        Key itemKey = item.getItemStack() == null ? Key.of("AIR:0") : Key.of(item.getItemStack());
        return !item.isDead() && !itemKey.toString().equals("AIR:0") &&
                item.getPickupDelay() < plugin.getSettings().maximumPickupDelay &&
                (chestData.getWhitelisted().isEmpty() || chestData.getWhitelisted().contains(itemKey)) &&
                !chestData.getBlacklisted().contains(itemKey);
    };

    public static List<ItemStack> fixItemStackAmount(ItemStack itemStack, int amount) {
        int maxStackSize = itemStack.getMaxStackSize();

        // https://github.com/BG-Software-LLC/WildChests/issues/272
        if(maxStackSize <= 0)
            maxStackSize = DEFAULT_MAX_STACK_SIZE;

        if (maxStackSize == DEFAULT_MAX_STACK_SIZE) {
            itemStack.setAmount(amount);
            return Collections.singletonList(itemStack);
        }

        if (amount <= maxStackSize)
            return Collections.singletonList(itemStack);

        int amountOfFullStacks = amount / maxStackSize;
        int amountOfLeftOvers = amount % maxStackSize;

        List<ItemStack> totalItems = new LinkedList<>();

        if (amountOfFullStacks > 0) {
            ItemStack fullStackItem = itemStack.clone();
            fullStackItem.setAmount(maxStackSize);
            for (int i = 0; i < amountOfFullStacks; ++i) {
                totalItems.add(fullStackItem.clone());
            }
        }

        if (amountOfLeftOvers > 0) {
            ItemStack leftOverItem = itemStack.clone();
            leftOverItem.setAmount(amountOfLeftOvers);
            totalItems.add(leftOverItem);
        }

        return totalItems;
    }

    public static void tryCraftChest(Chest chest) {
        Inventory[] pages = chest.getPages();

        Iterator<Map.Entry<Recipe, List<RecipeUtils.RecipeIngredient>>> recipes = ((WChestData) chest.getData()).getRecipeIngredients();
        List<ItemStack> toAdd = new ArrayList<>();

        while (recipes.hasNext()) {
            Map.Entry<Recipe, List<RecipeUtils.RecipeIngredient>> recipe = recipes.next();

            if (recipe.getValue().isEmpty())
                continue;

            int amountOfRecipes = Integer.MAX_VALUE;
            int pageSize = pages[0].getSize();
            Map<RecipeUtils.RecipeIngredient, List<Integer>> slots = new HashMap<>();

            for (RecipeUtils.RecipeIngredient ingredient : recipe.getValue()) {
                for (int i = 0; i < pages.length; i++) {
                    // Count items returns a list of slots and the total amount of the items in the slots.
                    Pair<List<Integer>, Integer> countResult = RecipeUtils.countItems(ingredient, pages[i], i * pageSize);
                    amountOfRecipes = Math.min(amountOfRecipes, countResult.value / ingredient.getAmount());
                    slots.put(ingredient, countResult.key);
                }
            }

            if (amountOfRecipes > 0) {
                // We can't use chest#removeItem due to a glitch with named items
                // We manually removing the items
                for (Map.Entry<RecipeUtils.RecipeIngredient, List<Integer>> entry : slots.entrySet()) {
                    int amountToRemove = entry.getKey().getAmount() * amountOfRecipes;
                    for (int slot : entry.getValue()) {
                        int page = slot / pageSize;
                        slot = slot % pageSize;

                        ItemStack itemStack = pages[page].getItem(slot);

                        if (itemStack.getAmount() > amountToRemove) {
                            itemStack.setAmount(itemStack.getAmount() - amountToRemove);
                            break;
                        } else {
                            amountToRemove -= itemStack.getAmount();
                            pages[page].setItem(slot, new ItemStack(Material.AIR));
                        }

                    }

                }

                ItemStack result = recipe.getKey().getResult().clone();
                result.setAmount(result.getAmount() * amountOfRecipes);
                toAdd.add(result);
                NotifierTask.addCrafting(chest.getPlacer(), result, result.getAmount());
            }
        }

        Map<Integer, ItemStack> toDrop = chest.addItems(toAdd.toArray(new ItemStack[]{}));
        ItemUtils.dropItems(chest.getLocation(), toDrop.values(), false);
    }

    public static void trySellChest(Chest chest) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(chest.getPlacer());
        try {
            plugin.getProviders().startSellingTask(player);
            for (Inventory inventory : chest.getPages()) {
                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack itemStack = inventory.getItem(i);
                    Counter itemCount = new Counter();
                    if (trySellItem(player, chest, itemStack, itemCount)) {
                        if (itemCount.get() <= 0) {
                            inventory.setItem(i, new ItemStack(Material.AIR));
                        } else {
                            itemStack.setAmount((int) itemCount.get());
                            inventory.setItem(i, itemStack);
                        }
                    }
                }
            }
        } finally {
            plugin.getProviders().stopSellingTask(player);
        }
    }

    public static boolean trySellItem(OfflinePlayer player, Chest chest, ItemStack toSell, Counter newItemAmount) {
        if (toSell == null || toSell.getType() == Material.AIR)
            return false;

        ProvidersHandler.TransactionResult<Double> transactionResult = plugin.getProviders().canSellItem(player, toSell);

        if (!transactionResult.isSuccess())
            return false;

        ChestData chestData = chest.getData();

        SellChestTaskEvent sellChestTaskEvent = new SellChestTaskEvent(chest, toSell, chestData.getMultiplier());
        Bukkit.getPluginManager().callEvent(sellChestTaskEvent);

        double finalPrice = transactionResult.getData() * sellChestTaskEvent.getMultiplier();

        if (finalPrice <= 0)
            return false;

        boolean successDeposit;

        if (plugin.getSettings().sellCommand.isEmpty()) {
            successDeposit = plugin.getProviders().depositPlayer(player, chestData.getDepositMethod(), finalPrice);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getSettings().sellCommand
                    .replace("{player-name}", player.getName())
                    .replace("{price}", String.valueOf(finalPrice)));
            successDeposit = true;
        }

        if (successDeposit) {
            NotifierTask.addTransaction(player.getUniqueId(), toSell, toSell.getAmount(), finalPrice);
            if (transactionResult.getTransaction() != null) {
                ItemStack transactItem = transactionResult.getTransaction().getItem();
                if (transactItem != toSell)
                    newItemAmount.increase(toSell.getAmount() - transactItem.getAmount());
                transactionResult.getTransaction().onTransact();
            }
        }

        return successDeposit;
    }

}
