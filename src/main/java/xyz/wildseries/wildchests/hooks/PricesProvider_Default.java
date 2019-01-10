package xyz.wildseries.wildchests.hooks;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.wildseries.wildchests.WildChestsPlugin;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public final class PricesProvider_Default implements PricesProvider {

    private static Map<String, Double> prices = new HashMap<>();

    public PricesProvider_Default(){
        WildChestsPlugin.log("- Couldn''t find any prices providers, using default one");
    }

    @Override
    public double getPrice(Player player, ItemStack itemStack) {
        //Checks for 'TYPE' item
        if(prices.containsKey(itemStack.getType().name()))
            return prices.get(itemStack.getType().name()) * itemStack.getAmount();
        //Checks for 'TYPE:DATA' item
        if(prices.containsKey(itemStack.getType().name() + ":" + itemStack.getDurability()))
            return prices.get(itemStack.getType().name() + ":" + itemStack.getDurability()) * itemStack.getAmount();
        //Couldn't find a price for this item
        return -1;
    }
}
