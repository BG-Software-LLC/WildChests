package com.bgsoftware.wildchests.utils;

import com.bgsoftware.common.reflection.ReflectMethod;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class RecipeUtils {

    private static final ReflectMethod<Map<Character, RecipeChoice>> SHAPRED_RECIPE_GET_CHOICE_MAP =
            new ReflectMethod<>(ShapedRecipe.class, Map.class, "getChoiceMap");
    private static final ReflectMethod<List<RecipeChoice>> SHAPELESS_RECIPE_GET_CHOICE_LIST =
            new ReflectMethod<>(ShapelessRecipe.class, Map.class, "getChoiceList");

    public static List<RecipeIngredient> getIngredients(Recipe recipe) {
        return recipe instanceof ShapedRecipe ? getIngredients((ShapedRecipe) recipe) :
                recipe instanceof ShapelessRecipe ? getIngredients((ShapelessRecipe) recipe) : Collections.emptyList();
    }

    public static Pair<List<Integer>, Integer> countItems(RecipeIngredient recipeIngredient, Inventory inventory, int offsetSlot) {
        List<Integer> slots = new LinkedList<>();
        int amount = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack != null && recipeIngredient.test(itemStack)) {
                amount += itemStack.getAmount();
                slots.add(offsetSlot + i);
            }
        }

        return new Pair<>(slots, amount);
    }

    private static List<RecipeIngredient> getIngredients(ShapedRecipe shapedRecipe) {
        List<RecipeIngredient> recipeIngredients;

        if (SHAPRED_RECIPE_GET_CHOICE_MAP.isValid()) {
            recipeIngredients = SHAPRED_RECIPE_GET_CHOICE_MAP.invoke(shapedRecipe)
                    .values().stream().map(RecipeIngredient::of).collect(Collectors.toList());
        } else {
            recipeIngredients = getIngredients(new LinkedList<>(shapedRecipe.getIngredientMap().values()));
        }

        return mergeIngredients(recipeIngredients);
    }

    private static List<RecipeIngredient> getIngredients(ShapelessRecipe shapelessRecipe) {
        List<RecipeIngredient> recipeIngredients;

        if (SHAPELESS_RECIPE_GET_CHOICE_LIST.isValid()) {
            recipeIngredients = SHAPELESS_RECIPE_GET_CHOICE_LIST.invoke(shapelessRecipe)
                    .stream().map(RecipeIngredient::of).collect(Collectors.toList());
        } else {
            recipeIngredients = getIngredients(shapelessRecipe.getIngredientList());
        }

        return mergeIngredients(recipeIngredients);
    }

    @SuppressWarnings("deprecation")
    private static List<RecipeIngredient> getIngredients(List<ItemStack> oldList) {
        Map<ItemStack, Integer> counts = new HashMap<>();
        List<ItemStack> ingredients = new LinkedList<>();

        for (ItemStack itemStack : oldList) {
            if (itemStack != null) {
                if (itemStack.getData().getData() < 0)
                    itemStack.setDurability((short) 0);
                counts.put(itemStack, counts.getOrDefault(itemStack, 0) + itemStack.getAmount());
            }
        }

        for (ItemStack ingredient : counts.keySet()) {
            ingredient.setAmount(counts.get(ingredient));
            ingredients.add(ingredient);
        }

        return ingredients.stream().map(RecipeIngredient::of).collect(Collectors.toList());
    }

    private static List<RecipeIngredient> mergeIngredients(List<RecipeIngredient> recipeIngredients) {
        List<RecipeIngredient> recipeIngredientsList = new ArrayList<>(recipeIngredients);
        List<RecipeIngredient> toRemove = new LinkedList<>();

        for (int i = 0; i < recipeIngredients.size(); i++) {
            RecipeIngredient current = recipeIngredients.get(i);
            for (int j = i + 1; j < recipeIngredients.size(); j++) {
                RecipeIngredient other = recipeIngredients.get(j);
                if (current.isSimilar(other)) {
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
        private final List<ItemStack> ingredients;
        private final Predicate<ItemStack> predicate;

        private RecipeIngredient(List<ItemStack> ingredients, Predicate<ItemStack> predicate) {
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

        public RecipeIngredient merge(RecipeIngredient other) {
            outer:
            for (ItemStack current : ingredients) {
                for (ItemStack otherItem : other.ingredients) {
                    if (current.isSimilar(otherItem)) {
                        current.setAmount(current.getAmount() + otherItem.getAmount());
                        continue outer;
                    }
                }
            }
            amount += other.getAmount();
            return this;
        }

        public boolean isSimilar(RecipeIngredient other) {
            return ingredients.stream().allMatch(other);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RecipeIngredient that = (RecipeIngredient) o;
            return ingredients.equals(that.ingredients);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ingredients);
        }

        public static RecipeIngredient of(ItemStack itemStack) {
            return new RecipeIngredient(Collections.singletonList(itemStack), itemStack::isSimilar);
        }

        @SuppressWarnings("deprecation")
        public static RecipeIngredient of(RecipeChoice recipeChoice) {
            if (recipeChoice instanceof RecipeChoice.ExactChoice)
                return new RecipeIngredient(((RecipeChoice.ExactChoice) recipeChoice).getChoices(), recipeChoice);
            else
                return new RecipeIngredient(((RecipeChoice.MaterialChoice) recipeChoice).getChoices().stream().map(ItemStack::new)
                        .collect(Collectors.toList()), recipeChoice);
        }

    }

}
