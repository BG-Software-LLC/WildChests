package xyz.wildseries.wildchests.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import xyz.wildseries.wildchests.Locale;
import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.api.objects.chests.Chest;
import xyz.wildseries.wildchests.key.KeySet;
import xyz.wildseries.wildchests.objects.exceptions.PlayerNotOnlineException;
import xyz.wildseries.wildchests.task.NotifierTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChestUtils {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    public static void tryCraftChest(Chest chest){
        if(Bukkit.isPrimaryThread()){
            new Thread(() -> tryCraftChest(chest)).start();
            return;
        }

        Inventory[] pages = chest.getPages();
        StringBuilder message = new StringBuilder();
        Player player = Bukkit.getPlayer(chest.getPlacer());

        if(!Locale.CRAFTED_ITEMS_HEADER.isEmpty() && player != null)
            message.append(Locale.CRAFTED_ITEMS_HEADER.getMessage());

        KeySet recipes = new KeySet(chest.getData().getRecipes());
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        List<ItemStack> toAdd = new ArrayList<>();

        int totalCraftedItems = 0;

        while(recipeIterator.hasNext()){
            Recipe recipe = recipeIterator.next();

            if(!recipes.contains(recipe.getResult()))
                continue;

            int craftedItemsAmount = 0;

            List<ItemStack> ingredients;

            //Get the ingredients for the recipe
            if (recipe instanceof ShapedRecipe) {
                ingredients = getIngredients(new ArrayList<>(((ShapedRecipe) recipe).getIngredientMap().values()));
            } else if (recipe instanceof ShapelessRecipe) {
                ingredients = getIngredients(((ShapelessRecipe) recipe).getIngredientList());
            } else continue;

            if (ingredients.isEmpty())
                continue;

            boolean canCraft;
            do {
                canCraft = false;

                //Check if all the ingredients exist
                outerLoop: for (ItemStack ingredient : ingredients) {
                    for(Inventory page : pages){
                        if (page.containsAtLeast(ingredient, ingredient.getAmount())) {
                            canCraft = true;
                            continue outerLoop;
                        }
                    }
                }

                if (canCraft) {
                    //Only if we can craft, we need to remove the items
                    outerLoop: for(ItemStack ingredient : ingredients) {
                        for(Inventory page : pages){
                            if(page.removeItem(ingredient).isEmpty())
                                break outerLoop;
                        }
                    }

                    //Add the recipe's result to the inventory
                    toAdd.add(recipe.getResult());
                    craftedItemsAmount += recipe.getResult().getAmount();
                }
            }while(canCraft);

            totalCraftedItems += craftedItemsAmount;

            if(craftedItemsAmount > 0)
                if(!Locale.CRAFTED_ITEMS_LINE.isEmpty() && player != null)
                    message.append("\n").append(Locale.CRAFTED_ITEMS_LINE.getMessage(craftedItemsAmount, recipe.getResult().getType()));
        }

        for(ItemStack itemStack : toAdd) {
            if(!ItemUtils.addToChest(chest, itemStack))
                chest.getLocation().getWorld().dropItemNaturally(chest.getLocation(), itemStack);
        }

        if(!Locale.CRAFTED_ITEMS_FOOTER.isEmpty() && player != null)
            message.append("\n").append(Locale.CRAFTED_ITEMS_FOOTER.getMessage(totalCraftedItems));

        if(!message.toString().isEmpty() && totalCraftedItems > 0 && player != null)
            player.sendMessage(message.toString());
    }

    public static void trySellChest(Chest chest){
        if(Bukkit.isPrimaryThread()){
            new Thread(() -> trySellChest(chest)).start();
            return;
        }

        Inventory[] pages = chest.getPages();
        UUID placer = chest.getPlacer();

        List<ItemStack> itemStacks = new ArrayList<>();
        for(Inventory page : pages)
            itemStacks.addAll(Arrays.asList(page.getContents()));

        Map<ItemStack, Integer> sortedItems = getSortedItems(itemStacks.toArray(new ItemStack[0]));

        for(ItemStack itemStack : sortedItems.keySet()){
            itemStack.setAmount(sortedItems.get(itemStack));

            try {
                double price = plugin.getProviders().trySellItem(placer, itemStack);

                if(price <= 0)
                    continue;

                NotifierTask.addTransaction(placer, itemStack, itemStack.getAmount(), price);
            }catch(PlayerNotOnlineException ignored){ }

            for (Inventory page : pages)
                page.removeItem(itemStack);
        }
    }

    private static Map<ItemStack, Integer> getSortedItems(ItemStack[] itemStacks){
        // <ItemStack, TotalAmount>
        Map<ItemStack, Integer> map = new HashMap<>();

        for(ItemStack itemStack : itemStacks){
            if(itemStack != null && itemStack.getType() != Material.AIR){
                ItemStack cloned = itemStack.clone();
                cloned.setAmount(1);
                map.put(cloned, map.getOrDefault(cloned, 0) + itemStack.getAmount());
            }
        }

        return map;
    }

    @SuppressWarnings("deprecation")
    private static List<ItemStack> getIngredients(List<ItemStack> oldList){
        Map<ItemStack, Integer> counts = new HashMap<>();
        List<ItemStack> ingredients = new ArrayList<>();

        for(ItemStack itemStack : oldList){
            if(itemStack != null) {
                if (itemStack.getData().getData() < 0)
                    itemStack.setDurability((short) 0);
                counts.put(itemStack, counts.getOrDefault(itemStack, 0) + itemStack.getAmount());
            }
        }

        for(ItemStack ingredient : counts.keySet()){
            ingredient.setAmount(counts.get(ingredient));
            ingredients.add(ingredient);
        }

        return ingredients;
    }

}
