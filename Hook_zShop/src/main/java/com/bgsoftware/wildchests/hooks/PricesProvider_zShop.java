package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.hooks.PricesProvider;
import fr.maxlego08.shop.api.ShopManager;
import fr.maxlego08.shop.api.button.buttons.ItemButton;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class PricesProvider_zShop implements PricesProvider {

    private final ShopManager shopManager;

    public PricesProvider_zShop(){
        WildChestsPlugin.log(" - Using zShop as PricesProvider.");

        RegisteredServiceProvider<ShopManager> provider = Bukkit.getServicesManager().getRegistration(ShopManager.class);
        shopManager = provider == null ? null : provider.getProvider();
    }

    @Override
    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        ItemButton itemButton = shopManager.getItemButton(itemStack).orElse(null);
        return itemButton == null ? 0 : offlinePlayer.isOnline() ? itemButton.getSellPrice(offlinePlayer.getPlayer()) : itemButton.getSellPrice();
    }

}
