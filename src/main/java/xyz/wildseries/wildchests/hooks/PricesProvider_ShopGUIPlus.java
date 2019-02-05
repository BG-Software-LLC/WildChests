package xyz.wildseries.wildchests.hooks;

import net.brcdev.shopgui.ShopGuiPlugin;
import net.brcdev.shopgui.player.PlayerData;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.wildseries.wildchests.WildChestsPlugin;

import java.util.Map;

public final class PricesProvider_ShopGUIPlus implements PricesProvider {

    private ShopGuiPlugin plugin;

    public PricesProvider_ShopGUIPlus(){
        WildChestsPlugin.log("- Using ShopGUIPlus as PricesProvider");
        //Loading database of online players
        plugin = ShopGuiPlugin.getInstance();
        for(Player player : Bukkit.getOnlinePlayers())
           plugin.getPlayerManager().loadData(player);
    }

    @Override
    public double getPrice(Player player, ItemStack itemStack) {
        try {
            PlayerData playerData;

            try{
                playerData = plugin.getPlayerManager().getPlayerData(player);
            }catch(NullPointerException ex){
                plugin.getPlayerManager().registerPlayer(player);
                playerData = plugin.getPlayerManager().getPlayerData(player);
            }

            double price = 0;

            Map<String, Shop> shops = plugin.getShopManager().shops;
            for(Shop shop : shops.values()){
                for(ShopItem shopItem : shop.getShopItems()){
                    if(shopItem.getItem().isSimilar(itemStack) && getSellPrice(shopItem, shop, playerData, player, itemStack.getAmount()) > price)
                        price = getSellPrice(shopItem, shop, playerData, player, itemStack.getAmount());
                }
            }
            return price;
        }catch(Exception ex){
            return -1;
        }
    }

    private double getSellPrice(ShopItem shopItem, Shop shop, PlayerData playerData, Player player, int amount){
        try {
            return shopItem.getSellPriceForAmount(shop, player, playerData, amount);
        }catch(NoSuchMethodError ex){
            try {
                //noinspection JavaReflectionMemberAccess
                return (double) ShopItem.class.getMethod("getSellPriceForAmount", Shop.class, PlayerData.class, int.class).invoke(shopItem, shop, playerData, amount);
            }catch(Exception ex1){
                ex1.printStackTrace();
                return 0;
            }
        }
    }
}
