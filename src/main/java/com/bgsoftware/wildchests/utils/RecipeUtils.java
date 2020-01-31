package com.bgsoftware.wildchests.utils;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class RecipeUtils {

    public static List<RecipeIngredient> getIngredients(ShapedRecipe shapedRecipe){
        try{
            //noinspection unchecked, JavaReflectionMemberAccess
            return ((Map<Character, RecipeChoice>) ShapedRecipe.class.getMethod("getChoiceMap").invoke(shapedRecipe)).values().stream().map(RecipeIngredient::of).collect(Collectors.toList());
        }catch(Exception ignored){
            return getIngredients(new ArrayList<>(shapedRecipe.getIngredientMap().values()));
        }
    }

    public static List<RecipeIngredient> getIngredients(ShapelessRecipe shapelessRecipe){
        try{
            //noinspection unchecked, JavaReflectionMemberAccess
            return ((List<RecipeChoice>) ShapelessRecipe.class.getMethod("getChoiceList").invoke(shapelessRecipe)).stream().map(RecipeIngredient::of).collect(Collectors.toList());
        }catch(Exception ignored){
            return getIngredients(shapelessRecipe.getIngredientList());
        }
    }

    public static int countItems(RecipeIngredient recipeIngredient, Inventory inventory){
        int amount = 0;

        for(ItemStack itemStack : inventory.getContents()){
            if(itemStack != null && recipeIngredient.test(itemStack))
                amount += itemStack.getAmount();
        }

        return amount;
    }

    @SuppressWarnings("deprecation")
    private static List<RecipeIngredient> getIngredients(List<ItemStack> oldList){
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

        return ingredients.stream().map(RecipeIngredient::of).collect(Collectors.toList());
    }

    public static final class RecipeIngredient implements Predicate<ItemStack> {

        private int amount;
        private List<ItemStack> ingredients;
        private Predicate<ItemStack> predicate;

        private RecipeIngredient(int amount, List<ItemStack> ingredients, Predicate<ItemStack> predicate){
            this.amount = amount;
            this.ingredients = ingredients;
            this.predicate = predicate;
        }

        @Override
        public boolean test(ItemStack itemStack) {
            return predicate.test(itemStack);
        }

        public int getAmount() {
            return amount;
        }

        public List<ItemStack> getIngredients() {
            return ingredients;
        }

        public static RecipeIngredient of(ItemStack itemStack){
            return new RecipeIngredient(itemStack.getAmount(), Collections.singletonList(itemStack), itemStack::isSimilar);
        }

        @SuppressWarnings("deprecation")
        public static RecipeIngredient of(RecipeChoice recipeChoice){
            if(recipeChoice instanceof RecipeChoice.ExactChoice)
                return new RecipeIngredient(recipeChoice.getItemStack().getAmount(), ((RecipeChoice.ExactChoice) recipeChoice).getChoices(), recipeChoice);
            else
                return new RecipeIngredient(recipeChoice.getItemStack().getAmount(),
                        ((RecipeChoice.MaterialChoice) recipeChoice).getChoices().stream().map(ItemStack::new).collect(Collectors.toList()), recipeChoice);
        }

    }

}
