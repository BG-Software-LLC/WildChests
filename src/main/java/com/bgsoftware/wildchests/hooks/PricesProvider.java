package com.bgsoftware.wildchests.hooks;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

public interface PricesProvider {

    CompletableFuture<Double> getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack);

}
