package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.utils.Pair;
import net.brcdev.shopgui.ShopGuiPlugin;
import net.brcdev.shopgui.player.PlayerData;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.ShopItem;
import net.brcdev.shopgui.util.ItemUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class PricesProvider_ShopGUIPlus implements PricesProvider {

    // Added cache for shop items for better performance
    private final Map<WrappedItemStack, Pair<ShopItem, Shop>> cachedShopItems = new HashMap<>();
    private final ShopGuiPlugin plugin;

    public PricesProvider_ShopGUIPlus(){
        WildChestsPlugin.log("- Using ShopGUIPlus as PricesProvider");
        plugin = ShopGuiPlugin.getInstance();
    }

    @Override
    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        Player onlinePlayer = offlinePlayer.getPlayer();

        double price = 0;

        WrappedItemStack wrappedItemStack = new WrappedItemStack(itemStack);
        Pair<ShopItem, Shop> shopPair = cachedShopItems.computeIfAbsent(wrappedItemStack, i -> {
            Map<String, Shop> shops = plugin.getShopManager().shops;
            for (Shop shop : shops.values()) {
                for (ShopItem _shopItem : shop.getShopItems())
                    if (ItemUtils.compareItemStacks(_shopItem.getItem(), itemStack, _shopItem.isCompareMeta()))
                        return new Pair<>(_shopItem, shop);
            }

            return null;
        });

        if(shopPair != null){
            if(onlinePlayer == null) {
                //noinspection deprecation
                price = Math.max(price, shopPair.key.getSellPriceForAmount(itemStack.getAmount()));
            }
            else{
                PlayerData playerData = ShopGuiPlugin.getInstance().getPlayerManager().getPlayerData(onlinePlayer);
                price = Math.max(price, shopPair.key.getSellPriceForAmount(onlinePlayer, playerData, itemStack.getAmount()));
            }
        }

        return price;
    }

    private static final class WrappedItemStack{

        private final ItemStack value;

        WrappedItemStack(ItemStack value){
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WrappedItemStack that = (WrappedItemStack) o;
            return value.isSimilar(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

}
