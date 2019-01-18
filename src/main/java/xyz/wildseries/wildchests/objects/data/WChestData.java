package xyz.wildseries.wildchests.objects.data;

import org.bukkit.inventory.ItemStack;
import xyz.wildseries.wildchests.api.objects.ChestType;
import xyz.wildseries.wildchests.api.objects.data.ChestData;
import xyz.wildseries.wildchests.api.objects.data.InventoryData;
import xyz.wildseries.wildchests.key.KeySet;

import java.util.ArrayList;
import java.util.HashMap;
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
    private KeySet recipes;
    private Map<Integer, InventoryData> pagesData;
    private int defaultPagesAmount;

    public WChestData(String name, ItemStack itemStack, ChestType chestType) {
        this.name = name;
        this.itemStack = itemStack;
        this.chestType = chestType.name();
        this.defaultSize = 9 * 3;
        this.defaultTitle = "Chest";
        this.sellMode = false;
        this.hopperFilter = false;
        this.recipes = new KeySet();
        this.pagesData = new HashMap<>();
        this.defaultPagesAmount = 1;
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
        return pagesData.containsKey(page) ? pagesData.get(page).getTitle() : defaultTitle;
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
        return !recipes.isEmpty();
    }

    @Override
    public List<String> getRecipes() {
        return new ArrayList<>(recipes.asStringSet());
    }

    public KeySet getRecipesSet(){
        return recipes;
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
        this.recipes = new KeySet(recipes);
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
    public String toString() {
        return "ChestData{" +
                "name=" + name + "," +
                "chestType=" + chestType + "," +
                "defaultSize=" + defaultSize + "," +
                "defaultTitle=" + defaultTitle + "," +
                "sellMode=" + sellMode + "," +
                "recipes=" + recipes +
                "}";
    }
}
