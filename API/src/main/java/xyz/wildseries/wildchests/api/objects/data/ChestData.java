package xyz.wildseries.wildchests.api.objects.data;

import org.bukkit.inventory.ItemStack;
import xyz.wildseries.wildchests.api.objects.ChestType;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public interface ChestData {

    String getName();

    ItemStack getItemStack();

    ChestType getChestType();

    int getDefaultSize();

    String getDefaultTitle();

    String getTitle(int page);

    boolean isSellMode();

    boolean isAutoCrafter();

    List<String> getRecipes();

    Map<Integer, InventoryData> getPagesData();

    int getDefaultPagesAmount();

    void setDefaultSize(int size);

    void setDefaultTitle(String title);

    void setSellMode(boolean sellMode);

    void setAutoCrafter(List<String> recipes);

    void setPagesData(Map<Integer, InventoryData> pagesData);

    void setDefaultPagesAmount(int defaultPagesAmount);

}
