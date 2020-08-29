package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.key.Key;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.events.SellChestTaskEvent;
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

public final class ChestUtils {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
    public static final short DEFAULT_COOLDOWN = 20;

    public static final BiPredicate<Item, ChestData> SUCTION_PREDICATE = (item, chestData) -> {
        Key itemKey = item.getItemStack() == null ? Key.of("AIR:0") : Key.of(item.getItemStack());
        return !item.isDead() && !itemKey.toString().equals("AIR:0") &&
                item.getPickupDelay() < plugin.getSettings().maximumPickupDelay &&
                (chestData.getWhitelisted().isEmpty() || chestData.getWhitelisted().contains(itemKey)) &&
                !chestData.getBlacklisted().contains(itemKey);
    };

    public static void tryCraftChest(Chest chest){
        Inventory[] pages = chest.getPages();

        Iterator<Map.Entry<Recipe, List<RecipeUtils.RecipeIngredient>>> recipes = ((WChestData) chest.getData()).getRecipeIngredients();
        List<ItemStack> toAdd = new ArrayList<>();

        while (recipes.hasNext()) {
            Map.Entry<Recipe, List<RecipeUtils.RecipeIngredient>> recipe = recipes.next();

            if (recipe.getValue().isEmpty())
                continue;

            int amountOfRecipes = Integer.MAX_VALUE;

            for (RecipeUtils.RecipeIngredient ingredient : recipe.getValue()) {
                for (Inventory page : pages) {
                    amountOfRecipes = Math.min(amountOfRecipes, RecipeUtils.countItems(ingredient, page) / ingredient.getAmount());
                }
            }

            if (amountOfRecipes > 0) {
                for (RecipeUtils.RecipeIngredient recipeIngredient : recipe.getValue()) {
                    for(ItemStack ingredient : recipeIngredient.getIngredients())
                        chest.removeItem(ingredient.getAmount() * amountOfRecipes, ingredient);
                }

                ItemStack result = recipe.getKey().getResult().clone();
                result.setAmount(result.getAmount() * amountOfRecipes);
                toAdd.add(result);
                NotifierTask.addCrafting(chest.getPlacer(), result, result.getAmount());
            }
        }

        List<ItemStack> toDrop = new ArrayList<>(chest.addItems(toAdd.toArray(new ItemStack[]{})).values());

        if (!toDrop.isEmpty()) {
            for (ItemStack itemStack : toDrop)
                ItemUtils.dropItem(chest.getLocation(), itemStack);
        }
    }

    public static void trySellChest(Chest chest){
        Arrays.stream(chest.getPages()).forEach(inventory -> {
            for(int i = 0; i < inventory.getSize(); i++){
                if(trySellItem(chest, inventory.getItem(i)))
                    inventory.setItem(i, new ItemStack(Material.AIR));
            }
        });
    }

    public static boolean trySellItem(Chest chest, ItemStack toSell){
        if(toSell == null || toSell.getType() == Material.AIR)
            return false;

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(chest.getPlacer());

        ProvidersHandler.TransactionResult<Double> transactionResult = plugin.getProviders().canSellItem(offlinePlayer, toSell);

        if (!transactionResult.isSuccess())
            return false;

        SellChestTaskEvent sellChestTaskEvent = new SellChestTaskEvent(chest, toSell, chest.getData().getMultiplier());
        Bukkit.getPluginManager().callEvent(sellChestTaskEvent);

        double finalPrice = transactionResult.getData() * sellChestTaskEvent.getMultiplier();

        if(finalPrice <= 0)
            return false;

        if (plugin.getSettings().sellCommand.isEmpty()) {
            plugin.getProviders().depositPlayer(offlinePlayer, finalPrice);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getSettings().sellCommand
                    .replace("{player-name}", offlinePlayer.getName())
                    .replace("{price}", String.valueOf(finalPrice)));
        }

        NotifierTask.addTransaction(offlinePlayer.getUniqueId(), toSell, toSell.getAmount(), finalPrice);

        return true;
    }

    public static ItemStack getRemainingItem(Map<Integer, ItemStack> additionalItems){
        return additionalItems.isEmpty() ? null : new ArrayList<>(additionalItems.values()).get(0);
    }

}
