package com.bgsoftware.wildchests.api.objects.data;

import com.bgsoftware.wildchests.api.key.Key;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import com.bgsoftware.wildchests.api.objects.ChestType;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public interface ChestData {

    String getName();

    ItemStack getItemStack();

    ChestType getChestType();

    int getDefaultSize();

    String getDefaultTitle();

    String getTitle(int page);

    boolean isSellMode();

    boolean isHopperFilter();

    boolean isAutoCrafter();

    Iterator<Recipe> getRecipes();

    boolean containsRecipe(ItemStack result);

    Map<Integer, InventoryData> getPagesData();

    int getDefaultPagesAmount();

    double getMultiplier();

    boolean isAutoCollect();

    boolean isAutoSuction();

    int getAutoSuctionRange();

    boolean isAutoSuctionChunk();

    Set<Key> getBlacklisted();

    Set<Key> getWhitelisted();

    BigInteger getStorageUnitMaxAmount();

    List<String> getChestParticles();

    void setDefaultSize(int size);

    void setDefaultTitle(String title);

    void setSellMode(boolean sellMode);

    void setHopperFilter(boolean hopperFilter);

    void setAutoCrafter(List<String> recipes);

    void setPagesData(Map<Integer, InventoryData> pagesData);

    void setDefaultPagesAmount(int defaultPagesAmount);

    void setMultiplier(double multiplier);

    void setAutoCollect(boolean autoCollect);

    void setAutoSuctionRange(int autoSuctionRange);

    void setAutoSuctionChunk(boolean autoSuctionChunk);

    void setBlacklisted(Set<Key> blacklisted);

    void setWhitelisted(Set<Key> whitelisted);

    void setStorageUnitMaxAmount(BigInteger maxAmount);

    void setParticles(List<String> particles);

}
