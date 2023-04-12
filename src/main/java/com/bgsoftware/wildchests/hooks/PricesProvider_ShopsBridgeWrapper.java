package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.common.shopsbridge.IShopsBridge;
import com.bgsoftware.common.shopsbridge.ShopsProvider;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.hooks.PricesProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

public class PricesProvider_ShopsBridgeWrapper implements PricesProvider {

    private final IShopsBridge shopsBridge;

    public PricesProvider_ShopsBridgeWrapper(ShopsProvider shopsProvider, IShopsBridge shopsBridge) {
        WildChestsPlugin.log(" - Using " + shopsProvider.getPluginName() + " as PricesProvider.");
        this.shopsBridge = shopsBridge;
    }

    @Override
    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        return this.shopsBridge.getSellPrice(offlinePlayer, itemStack).doubleValue();
    }

}
