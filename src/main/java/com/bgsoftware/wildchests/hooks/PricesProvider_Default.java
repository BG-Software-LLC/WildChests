package com.bgsoftware.wildchests.hooks;

import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import com.bgsoftware.wildchests.WildChestsPlugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public final class PricesProvider_Default implements PricesProvider {

    public static Map<String, Double> prices = new ConcurrentHashMap<>();

    public PricesProvider_Default(){
        WildChestsPlugin.log("- Couldn''t find any prices providers, using default one");
    }

    @Override
    public CompletableFuture<Double> getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        //Checks for 'TYPE' item
        if(prices.containsKey(itemStack.getType().name()))
            return CompletableFuture.completedFuture(prices.get(itemStack.getType().name()) * itemStack.getAmount());
        //Checks for 'TYPE:DATA' item
        if(prices.containsKey(itemStack.getType().name() + ":" + itemStack.getDurability()))
            return CompletableFuture.completedFuture(prices.get(itemStack.getType().name() + ":" + itemStack.getDurability()) * itemStack.getAmount());
        //Couldn't find a price for this item
        return CompletableFuture.completedFuture(-1D);
    }
}
