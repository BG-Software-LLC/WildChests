package xyz.wildseries.wildchests.handlers;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import xyz.wildseries.wildchests.hooks.PricesProvider;
import xyz.wildseries.wildchests.hooks.PricesProvider_Default;
import xyz.wildseries.wildchests.hooks.PricesProvider_Essentials;
import xyz.wildseries.wildchests.hooks.PricesProvider_ShopGUIPlus;
import xyz.wildseries.wildchests.objects.exceptions.PlayerNotOnlineException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class ProvidersHandler {

    private final Map<UUID, List<ItemStack>> awaitingItems = new HashMap<>();

    private boolean isVaultEnabled;
    private Economy economy;

    private final PricesProvider pricesProvider;

    public ProvidersHandler(){
        if(Bukkit.getPluginManager().isPluginEnabled("ShopGUIPlus"))
            pricesProvider = new PricesProvider_ShopGUIPlus();
        else if(Bukkit.getPluginManager().isPluginEnabled("Essentials"))
            pricesProvider = new PricesProvider_Essentials();
        else pricesProvider = new PricesProvider_Default();
    }

    /*
     * Hooks' methods
     */

    public double getPrice(Player player, ItemStack itemStack){
        return pricesProvider.getPrice(player, itemStack);
    }

    /*
     * Handler's methods
     */

    public void enableVault(){
        isVaultEnabled = true;
        economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
    }

    public int tryDepositMoney(Player player){
        if(!awaitingItems.containsKey(player.getUniqueId()))
            return 0;

        List<ItemStack> items = awaitingItems.get(player.getUniqueId());
        awaitingItems.remove(player.getUniqueId());

        if(!isVaultEnabled)
            return 0;

        int totalPrice = 0;

        for(ItemStack itemStack : items)
            totalPrice += getPrice(player, itemStack);

        if(!economy.hasAccount(player))
            economy.createPlayerAccount(player);

        economy.depositPlayer(player, totalPrice);

        return totalPrice;
    }

    public double trySellItem(UUID placer, ItemStack itemStack) throws PlayerNotOnlineException {
        double price = 0;

        if(!canSellItem(placer, itemStack))
            return price;

        //If item can be sold, the player is online for sure.
        Player player = Bukkit.getPlayer(placer);

        price = getPrice(player, itemStack);

        if(price > 0) {
            if (!economy.hasAccount(player))
                economy.createPlayerAccount(player);

            economy.depositPlayer(player, price);
        }

        return price;
    }

    public boolean canSellItem(UUID playerUUID, ItemStack itemStack) throws PlayerNotOnlineException{
        if(itemStack == null)
            return false;
        if(Bukkit.getPlayer(playerUUID) == null){
            if(!awaitingItems.containsKey(playerUUID))
                awaitingItems.put(playerUUID, new ArrayList<>());
            awaitingItems.get(playerUUID).add(itemStack);
            throw new PlayerNotOnlineException();
        }
        return isVaultEnabled && getPrice(Bukkit.getPlayer(playerUUID), itemStack) > 0;
    }

    public boolean transactionSuccess(Player player, double money){
        if(!economy.hasAccount(player))
            economy.createPlayerAccount(player);
        return economy.withdrawPlayer(player, money).transactionSuccess();
    }

    public boolean isVaultEnabled(){
        return isVaultEnabled;
    }

}
