package com.bgsoftware.wildchests.hooks;

import net.brcdev.shopgui.ShopGuiPlugin;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.ShopItem;

import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import com.bgsoftware.wildchests.WildChestsPlugin;

import java.util.Map;

public final class PricesProvider_ShopGUIPlus implements PricesProvider {

    private ShopGuiPlugin plugin;

    public PricesProvider_ShopGUIPlus(){
        WildChestsPlugin.log("- Using ShopGUIPlus as PricesProvider");
        plugin = ShopGuiPlugin.getInstance();
    }

    @Override
    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        double price = 0;

        Map<String, Shop> shops = plugin.getShopManager().shops;
        for(Shop shop : shops.values()){
            for(ShopItem shopItem : shop.getShopItems()){
                if(shopItem.getItem().isSimilar(itemStack)) {
                    //noinspection deprecation
                    double shopPrice = shopItem.getSellPriceForAmount(itemStack.getAmount());
                    if(shopPrice > price)
                        price = shopPrice;
                }
            }
        }
        return price;
    }
}
