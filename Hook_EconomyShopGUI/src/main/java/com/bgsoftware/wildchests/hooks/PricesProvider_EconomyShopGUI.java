package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.hooks.PricesProvider;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("unused")
public final class PricesProvider_EconomyShopGUI implements PricesProvider {

    public PricesProvider_EconomyShopGUI(){
        WildChestsPlugin.log(" - Using EconomyShopGUI as PricesProvider.");
    }

    @Override
    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        Player player = offlinePlayer.getPlayer();
        return player == null ? EconomyShopGUIHook.getItemSellPrice(itemStack) :
                EconomyShopGUIHook.getItemSellPrice(player, itemStack);
    }

}
