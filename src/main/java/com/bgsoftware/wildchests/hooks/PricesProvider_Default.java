package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.api.hooks.PricesProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public final class PricesProvider_Default implements PricesProvider {

    public static Map<String, Double> prices = new ConcurrentHashMap<>();

    @Override
    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
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
