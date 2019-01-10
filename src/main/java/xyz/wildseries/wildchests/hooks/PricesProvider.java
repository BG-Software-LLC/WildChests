package xyz.wildseries.wildchests.hooks;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PricesProvider {

    double getPrice(Player player, ItemStack itemStack);

}
