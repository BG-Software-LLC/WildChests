package xyz.wildseries.wildchests.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.api.objects.chests.Chest;
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
        Player player = Bukkit.getPlayer(chest.getPlacer());

        Iterator<Recipe> recipes = chest.getData().getRecipes();
        List<ItemStack> toAdd = new ArrayList<>();

        while(recipes.hasNext()){
            Recipe recipe = recipes.next();
            List<ItemStack> ingredients;

            //Get the ingredients for the recipe
            if (recipe instanceof ShapedRecipe) {
                ingredients = getIngredients(new ArrayList<>(((ShapedRecipe) recipe).getIngredientMap().values()));
            } else if (recipe instanceof ShapelessRecipe) {
                ingredients = getIngredients(((ShapelessRecipe) recipe).getIngredientList());
            } else continue;

            if (ingredients.isEmpty())
                continue;

            int amountOfRecipes = Integer.MAX_VALUE;

            for(ItemStack ingredient : ingredients){
                for(Inventory page : pages){
                    amountOfRecipes = Math.min(amountOfRecipes, ItemUtils.countItems(ingredient, page) / ingredient.getAmount());
                }
            }

            if(amountOfRecipes > 0) {
                for (ItemStack ingredient : ingredients) {
                    ItemUtils.removeFromChest(chest, ingredient, ingredient.getAmount() * amountOfRecipes);
                }
                ItemStack result = recipe.getResult().clone();
                result.setAmount(result.getAmount() * amountOfRecipes);
                toAdd.add(result);
                NotifierTask.addCrafting(player.getUniqueId(), result, result.getAmount());
            }
        }

        List<ItemStack> toDrop = new ArrayList<>();

        for(ItemStack itemStack : toAdd) {
            if(!ItemUtils.addToChest(chest, itemStack)) {
                toDrop.add(itemStack);
            }
        }

        if(!toDrop.isEmpty()){
            Bukkit.getScheduler().runTask(plugin, () -> {
                for(ItemStack itemStack : toDrop)
                    chest.getLocation().getWorld().dropItemNaturally(chest.getLocation(), itemStack);
            });
        }
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
                double price = plugin.getProviders().getPrice(placer, itemStack);

                if(price <= 0)
                    continue;

                if(plugin.getSettings().sellCommand.isEmpty()) {
                    plugin.getProviders().trySellItem(placer, itemStack);
                }else{
                    Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getSettings().sellCommand
                                .replace("{player-name}", Bukkit.getPlayer(placer).getName())
                                .replace("{price}", String.valueOf(price))));
                }

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
