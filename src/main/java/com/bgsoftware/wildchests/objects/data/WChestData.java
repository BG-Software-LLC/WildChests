package com.bgsoftware.wildchests.objects.data;

import com.google.common.collect.Iterators;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.api.objects.data.InventoryData;
import com.bgsoftware.wildchests.key.KeySet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class WChestData implements ChestData {

    private final String name;
    private final ItemStack itemStack;
    private final String chestType;

    private int defaultSize;
    private String defaultTitle;
    private boolean sellMode;
    private boolean hopperFilter;
    private List<Recipe> recipes;
    private Map<Integer, InventoryData> pagesData;
    private int defaultPagesAmount;
    private double multiplier;
    private boolean autoCollect;

    //Storage Units only!
    private BigInteger maxAmount;

    public WChestData(String name, ItemStack itemStack, ChestType chestType) {
        this.name = name;
        this.itemStack = itemStack;
        this.chestType = chestType.name();
        this.defaultSize = 9 * 3;
        this.defaultTitle = "Chest";
        this.sellMode = false;
        this.hopperFilter = false;
        this.recipes = new ArrayList<>();
        this.pagesData = new HashMap<>();
        this.defaultPagesAmount = 1;
        this.multiplier = 1;
        this.maxAmount = BigInteger.valueOf(-1);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    @Override
    public ChestType getChestType() {
        return ChestType.valueOf(chestType);
    }

    @Override
    public int getDefaultSize() {
        return defaultSize;
    }

    @Override
    public String getDefaultTitle() {
        return defaultTitle;
    }

    @Override
    public String getTitle(int page) {
        String title = pagesData.containsKey(page) ? pagesData.get(page).getTitle() : defaultTitle;
        if(title.length() >= 32) title = title.substring(0, 31);
        return title;
    }

    @Override
    public boolean isSellMode(){
        return sellMode;
    }

    @Override
    public boolean isHopperFilter() {
        return hopperFilter;
    }

    @Override
    public boolean isAutoCrafter() {
        return Iterators.size(getRecipes()) != 0;
    }

    @Override
    public Iterator<Recipe> getRecipes() {
        return Iterators.unmodifiableIterator(recipes.iterator());
    }

    @Override
    public boolean containsRecipe(ItemStack result) {
        Iterator<Recipe> recipes = getRecipes();
        boolean contains = false;

        while(recipes.hasNext()){
            Recipe recipe = recipes.next();
            if(recipe.getResult().isSimilar(result))
                contains = true;
        }

        return contains;
    }

    @Override
    public Map<Integer, InventoryData> getPagesData() {
        return new HashMap<>(pagesData);
    }

    @Override
    public int getDefaultPagesAmount() {
        return defaultPagesAmount;
    }

    @Override
    public double getMultiplier() {
        return multiplier;
    }

    @Override
    public boolean isAutoCollect() {
        return autoCollect;
    }

    @Override
    public BigInteger getStorageUnitMaxAmount() {
        if(ChestType.valueOf(chestType) != ChestType.STORAGE_UNIT)
            throw new UnsupportedOperationException("Cannot get max amount of an unknown storage unit.");

        return maxAmount;
    }

    @Override
    public void setDefaultSize(int size) {
        this.defaultSize = size;
    }

    @Override
    public void setDefaultTitle(String title) {
        this.defaultTitle = title;
    }

    @Override
    public void setSellMode(boolean sellMode){
        this.sellMode = sellMode;
    }

    @Override
    public void setHopperFilter(boolean hopperFilter) {
        this.hopperFilter = hopperFilter;
    }

    @Override
    public void setAutoCrafter(List<String> recipes) {
        KeySet recipesSet = new KeySet(recipes);
        List<Recipe> recipesToAdd = new ArrayList<>();
        Iterator<Recipe> bukkitRecipes = Bukkit.recipeIterator();

        while(bukkitRecipes.hasNext()){
            Recipe recipe = bukkitRecipes.next();
            if(recipesSet.contains(recipe.getResult()))
                recipesToAdd.add(recipe);
        }

        this.recipes.addAll(recipesToAdd);
    }

    @Override
    public void setPagesData(Map<Integer, InventoryData> pagesData) {
        this.pagesData = new HashMap<>(pagesData);
    }

    @Override
    public void setDefaultPagesAmount(int defaultPagesAmount) {
        this.defaultPagesAmount = defaultPagesAmount;
    }

    @Override
    public void setMultiplier(double multiplier) {
        this.multiplier = Math.max(0, multiplier);
    }

    @Override
    public void setAutoCollect(boolean autoCollect) {
        this.autoCollect = autoCollect;
    }

    @Override
    public void setStorageUnitMaxAmount(BigInteger maxAmount) {
        if(ChestType.valueOf(chestType) != ChestType.STORAGE_UNIT)
            throw new UnsupportedOperationException("Cannot set max amount of an unknown storage unit.");

        this.maxAmount = maxAmount;
    }

    @Override
    public String toString() {
        return "ChestData{" +
                "name=" + name + "," +
                "chestType=" + chestType + "," +
                "defaultSize=" + defaultSize + "," +
                "defaultTitle=" + defaultTitle + "," +
                "sellMode=" + sellMode + "," +
                "recipes=" + getRecipes() +
                "}";
    }
}
