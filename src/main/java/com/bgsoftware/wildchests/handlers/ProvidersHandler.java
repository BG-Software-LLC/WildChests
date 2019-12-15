package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.objects.exceptions.PlayerNotOnlineException;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.hooks.PricesProvider;
import com.bgsoftware.wildchests.hooks.PricesProvider_Default;
import com.bgsoftware.wildchests.hooks.PricesProvider_Essentials;
import com.bgsoftware.wildchests.hooks.PricesProvider_ShopGUIPlus;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class ProvidersHandler {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private boolean isVaultEnabled;
    private Economy economy;

    private final PricesProvider pricesProvider;

    public ProvidersHandler(){
        switch (plugin.getSettings().pricesProvider.toUpperCase()){
            case "SHOPGUIPLUS":
                if(Bukkit.getPluginManager().isPluginEnabled("ShopGUIPlus")) {
                    pricesProvider = new PricesProvider_ShopGUIPlus();
                    break;
                }
            case "ESSENTIALS":
                if(Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
                    pricesProvider = new PricesProvider_Essentials();
                    break;
                }
            default:
                pricesProvider = new PricesProvider_Default();
        }

        if(!initVault()){
            WildChestsPlugin.log("");
            WildChestsPlugin.log("If you want sell-chests to be enabled, please install Vault & Economy plugin.");
            WildChestsPlugin.log("");
        }
    }

    /*
     * Hooks' methods
     */

    public double getPrice(Player player, ItemStack itemStack, double multiplier){
        return pricesProvider.getPrice(player, itemStack) * multiplier;
    }

    /*
     * Handler's methods
     */

    public void enableVault(){
        isVaultEnabled = true;
        economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
    }

    private boolean initVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);

        if (rsp == null || rsp.getProvider() == null)
            return false;

        enableVault();

        return true;
    }

    public double getPrice(UUID placer, ItemStack itemStack, double multiplier) throws PlayerNotOnlineException {
        double price = 0;

        if(!canSellItem(placer, itemStack, multiplier))
            return price;

        //If item can be sold, the player is online for sure.
        Player player = Bukkit.getPlayer(placer);
        return pricesProvider.getPrice(player, itemStack) * multiplier;
    }

    @SuppressWarnings("UnusedReturnValue")
    public double trySellItem(UUID placer, ItemStack itemStack, double multiplier) throws PlayerNotOnlineException {
        double price = 0;

        if(!canSellItem(placer, itemStack, multiplier))
            return price;

        //If item can be sold, the player is online for sure.
        Player player = Bukkit.getPlayer(placer);

        price = getPrice(player, itemStack, multiplier);

        if(price > 0) {
            if (!economy.hasAccount(player))
                economy.createPlayerAccount(player);
            
            economy.depositPlayer(player, price);
        }

        return price;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canSellItem(UUID playerUUID, ItemStack itemStack, double multiplier) throws PlayerNotOnlineException{
        if(itemStack == null)
            return false;

        if(Bukkit.getPlayer(playerUUID) == null){
            plugin.getOfflinePayments().addItem(playerUUID, itemStack, multiplier);
            throw new PlayerNotOnlineException();
        }

        return isVaultEnabled && getPrice(Bukkit.getPlayer(playerUUID), itemStack, multiplier) > 0;
    }

    public boolean withdrawPlayer(Player player, double money){
        if(!economy.hasAccount(player))
            economy.createPlayerAccount(player);

        return economy.withdrawPlayer(player, money).transactionSuccess();
    }

    public boolean depositPlayer(Player player, double money){
        if(!economy.hasAccount(player))
            economy.createPlayerAccount(player);

        return economy.depositPlayer(player, money).transactionSuccess();
    }

    public boolean isVaultEnabled(){
        return isVaultEnabled;
    }

}
