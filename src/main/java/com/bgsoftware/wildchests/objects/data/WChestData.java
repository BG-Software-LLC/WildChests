package com.bgsoftware.wildchests.objects.data;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.key.Key;
import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.api.objects.DepositMethod;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.api.objects.data.InventoryData;
import com.bgsoftware.wildchests.key.KeySet;
import com.bgsoftware.wildchests.utils.RecipeUtils;
import com.google.common.collect.Iterators;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WChestData implements ChestData {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private final String name;
    private final String chestType;

    private ItemStack itemStack;
    private int defaultSize;
    private String defaultTitle;
    private boolean sellMode;
    private DepositMethod depositMethod;
    private boolean hopperFilter;
    private Map<Recipe, List<RecipeUtils.RecipeIngredient>> recipes;
    private Map<Integer, InventoryData> pagesData;
    private int defaultPagesAmount;
    private double multiplier;
    private boolean autoCollect;
    private int autoSuctionRange;
    private boolean autoSuctionChunk;
    private KeySet blacklisted, whitelisted;
    private List<String> particles;

    //Storage Units only!
    private BigInteger maxAmount;

    public WChestData(String name, ItemStack itemStack, ChestType chestType) {
        this.name = name;
        this.itemStack = itemStack;
        this.chestType = chestType.name();
        this.defaultSize = 9 * 3;
        this.defaultTitle = "Chest";
        this.sellMode = false;
        this.depositMethod = DepositMethod.VAULT;
        this.hopperFilter = false;
        this.recipes = new HashMap<>();
        this.pagesData = new HashMap<>();
        this.defaultPagesAmount = 1;
        this.multiplier = 1;
        this.maxAmount = BigInteger.valueOf(-1);
        this.autoSuctionRange = -1;
        this.autoSuctionChunk = false;
        this.blacklisted = new KeySet();
        this.whitelisted = new KeySet();
        this.particles = Collections.emptyList();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ItemStack getItemStack() {
        return plugin.getNMSAdapter().setChestName(itemStack, name);
    }

    public ItemStack getItemRaw() {
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
        if (title.length() >= 32) title = title.substring(0, 31);
        return title;
    }

    @Override
    public boolean isSellMode() {
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
        return Iterators.unmodifiableIterator(recipes.keySet().iterator());
    }

    public Iterator<Map.Entry<Recipe, List<RecipeUtils.RecipeIngredient>>> getRecipeIngredients() {
        return Iterators.unmodifiableIterator(recipes.entrySet().iterator());
    }

    @Override
    public boolean containsRecipe(ItemStack result) {
        Iterator<Recipe> recipes = getRecipes();
        boolean contains = false;

        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (recipe.getResult().isSimilar(result))
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
    public boolean isAutoSuction() {
        return autoSuctionRange > 0;
    }

    @Override
    public int getAutoSuctionRange() {
        return Math.max(1, autoSuctionRange);
    }

    @Override
    public boolean isAutoSuctionChunk() {
        return autoSuctionChunk;
    }

    @Override
    public DepositMethod getDepositMethod() {
        return depositMethod;
    }

    @Override
    public Set<Key> getBlacklisted() {
        return blacklisted;
    }

    @Override
    public Set<Key> getWhitelisted() {
        return whitelisted;
    }

    @Override
    public BigInteger getStorageUnitMaxAmount() {
        if (ChestType.valueOf(chestType) != ChestType.STORAGE_UNIT)
            throw new UnsupportedOperationException("Cannot get max amount of an unknown storage unit.");

        return maxAmount;
    }

    @Override
    public List<String> getChestParticles() {
        return particles;
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
    public void setSellMode(boolean sellMode) {
        this.sellMode = sellMode;
    }

    @Override
    public void setDepositMethod(DepositMethod depositMethod) {
        this.depositMethod = depositMethod;
    }

    @Override
    public void setHopperFilter(boolean hopperFilter) {
        this.hopperFilter = hopperFilter;
    }

    @Override
    public void setAutoCrafter(List<String> recipes) {
        Iterator<Recipe> bukkitRecipes = Bukkit.recipeIterator();
        KeySet recipesSet = new KeySet(recipes);

        while (bukkitRecipes.hasNext()) {
            Recipe recipe = bukkitRecipes.next();
            if (recipesSet.contains(recipe.getResult()))
                this.recipes.put(recipe, RecipeUtils.getIngredients(recipe));
        }
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
    public void setAutoSuctionRange(int autoSuctionRange) {
        this.autoSuctionRange = Math.max(1, autoSuctionRange);
    }

    @Override
    public void setAutoSuctionChunk(boolean autoSuctionChunk) {
        this.autoSuctionChunk = autoSuctionChunk;
    }

    @Override
    public void setBlacklisted(Set<Key> blacklisted) {
        this.blacklisted.addAll(blacklisted);
    }

    @Override
    public void setWhitelisted(Set<Key> whitelisted) {
        this.whitelisted.addAll(whitelisted);
    }

    @Override
    public void setStorageUnitMaxAmount(BigInteger maxAmount) {
        if (ChestType.valueOf(chestType) != ChestType.STORAGE_UNIT)
            throw new UnsupportedOperationException("Cannot set max amount of an unknown storage unit.");

        this.maxAmount = maxAmount;
    }

    @Override
    public void setParticles(List<String> particles) {
        this.particles = Collections.unmodifiableList(particles);
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

    public void loadFromData(WChestData chestData) {
        this.itemStack = chestData.itemStack;
        this.defaultSize = chestData.defaultSize;
        this.defaultTitle = chestData.defaultTitle;
        this.sellMode = chestData.sellMode;
        this.hopperFilter = chestData.hopperFilter;
        this.recipes = chestData.recipes;
        this.pagesData = chestData.pagesData;
        this.defaultPagesAmount = chestData.defaultPagesAmount;
        this.multiplier = chestData.multiplier;
        this.autoCollect = chestData.autoCollect;
        this.autoSuctionRange = chestData.autoSuctionRange;
        this.autoSuctionChunk = chestData.autoSuctionChunk;
        this.blacklisted = chestData.blacklisted;
        this.whitelisted = chestData.whitelisted;
        this.particles = chestData.particles;
        this.maxAmount = chestData.maxAmount;
    }

}
