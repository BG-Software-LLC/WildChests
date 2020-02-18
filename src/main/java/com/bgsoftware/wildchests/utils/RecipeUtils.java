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
        List<RecipeIngredient> recipeIngredients;

        try{
            //noinspection unchecked, JavaReflectionMemberAccess
            recipeIngredients = ((Map<Character, RecipeChoice>) ShapedRecipe.class.getMethod("getChoiceMap").invoke(shapedRecipe)).values().stream().map(RecipeIngredient::of).collect(Collectors.toList());
        }catch(Exception ignored){
            recipeIngredients = getIngredients(new ArrayList<>(shapedRecipe.getIngredientMap().values()));
        }

        return sortIngredients(recipeIngredients);
    }

    public static List<RecipeIngredient> getIngredients(ShapelessRecipe shapelessRecipe){
        List<RecipeIngredient> recipeIngredients;

        try{
            //noinspection unchecked, JavaReflectionMemberAccess
            recipeIngredients = ((List<RecipeChoice>) ShapelessRecipe.class.getMethod("getChoiceList").invoke(shapelessRecipe)).stream().map(RecipeIngredient::of).collect(Collectors.toList());
        }catch(Exception ignored){
            recipeIngredients = getIngredients(shapelessRecipe.getIngredientList());
        }

        return sortIngredients(recipeIngredients);
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

    private static List<RecipeIngredient> sortIngredients(List<RecipeIngredient> recipeIngredients){
        List<RecipeIngredient> recipeIngredientsList = new ArrayList<>(recipeIngredients);
        List<RecipeIngredient> toRemove = new ArrayList<>();

        for(int i = 0; i < recipeIngredients.size(); i++){
            RecipeIngredient current = recipeIngredients.get(i);
            for(int j = i + 1; j < recipeIngredients.size(); j++){
                RecipeIngredient other = recipeIngredients.get(j);
                if(current.isSimilar(other)) {
                    recipeIngredientsList.set(i, current.merge(other));
                    toRemove.add(other);
                }
            }
        }

        recipeIngredientsList.removeAll(toRemove);

        return recipeIngredientsList;
    }

    public static final class RecipeIngredient implements Predicate<ItemStack> {

        private int amount;
        private List<ItemStack> ingredients;
        private Predicate<ItemStack> predicate;

        private RecipeIngredient(List<ItemStack> ingredients, Predicate<ItemStack> predicate){
            this.ingredients = ingredients;
            this.amount = ingredients.get(0).getAmount();
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

        @Override
        public String toString() {
            return "RecipeIngredient{ingredients=" + ingredients + ",amount=" + amount + "}";
        }

        public RecipeIngredient merge(RecipeIngredient other){
            outer:
            for(ItemStack current : ingredients){
                for(ItemStack otherItem : other.ingredients){
                    if(current.isSimilar(otherItem)){
                        current.setAmount(current.getAmount() + otherItem.getAmount());
                        continue outer;
                    }
                }
            }
            amount += other.getAmount();
            return this;
        }

        public boolean isSimilar(RecipeIngredient other){
            return ingredients.stream().allMatch(other);
        }

        public static RecipeIngredient of(ItemStack itemStack){
            return new RecipeIngredient(Collections.singletonList(itemStack), itemStack::isSimilar);
        }

        @SuppressWarnings("deprecation")
        public static RecipeIngredient of(RecipeChoice recipeChoice){
            if(recipeChoice instanceof RecipeChoice.ExactChoice)
                return new RecipeIngredient(((RecipeChoice.ExactChoice) recipeChoice).getChoices(), recipeChoice);
            else
                return new RecipeIngredient(((RecipeChoice.MaterialChoice) recipeChoice).getChoices().stream().map(ItemStack::new)
                        .collect(Collectors.toList()), recipeChoice);
        }

    }

}
