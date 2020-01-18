package com.bgsoftware.wildchests.hooks;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PricesProvider {

    double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack);

}
