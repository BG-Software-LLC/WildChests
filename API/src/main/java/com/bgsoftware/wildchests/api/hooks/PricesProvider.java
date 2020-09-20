package com.bgsoftware.wildchests.api.hooks;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

public interface PricesProvider {

    double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack);

}
