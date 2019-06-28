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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class ProvidersHandler {

    private final Map<UUID, List<ItemStack>> awaitingItems = new HashMap<>();
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

    public double tryDepositMoney(Player player){
        if(!awaitingItems.containsKey(player.getUniqueId()))
            return 0;

        List<ItemStack> items = awaitingItems.get(player.getUniqueId());
        awaitingItems.remove(player.getUniqueId());

        if(!isVaultEnabled)
            return 0;

        BigDecimal totalPrice = BigDecimal.ZERO;

        for(ItemStack itemStack : items)
            totalPrice = totalPrice.add(BigDecimal.valueOf(getPrice(player, itemStack)));

        if(plugin.getSettings().sellCommand.isEmpty()) {
            if (!economy.hasAccount(player))
                economy.createPlayerAccount(player);

            final BigDecimal TOTAL_PRICE = totalPrice;
            Bukkit.getScheduler().runTask(plugin, () -> economy.depositPlayer(player, TOTAL_PRICE.doubleValue()));
        }
        else{
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getSettings().sellCommand
                    .replace("{player-name}", player.getName())
                    .replace("{price}", String.valueOf(totalPrice)));
        }

        return totalPrice.doubleValue();
    }

    public double getPrice(UUID placer, ItemStack itemStack) throws PlayerNotOnlineException {
        double price = 0;

        if(!canSellItem(placer, itemStack))
            return price;

        //If item can be sold, the player is online for sure.
        Player player = Bukkit.getPlayer(placer);
        return pricesProvider.getPrice(player, itemStack);
    }

    @SuppressWarnings("UnusedReturnValue")
    public double trySellItem(UUID placer, ItemStack itemStack, double multiplier) throws PlayerNotOnlineException {
        double price = 0;

        if(!canSellItem(placer, itemStack))
            return price;

        //If item can be sold, the player is online for sure.
        Player player = Bukkit.getPlayer(placer);

        price = getPrice(player, itemStack) * multiplier;

        if(price > 0) {
            if (!economy.hasAccount(player))
                economy.createPlayerAccount(player);

            final double PRICE = price;
            Bukkit.getScheduler().runTask(plugin, () -> economy.depositPlayer(player, PRICE));
        }

        return price;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

        if(economy.getBalance(player) >= money){
            Bukkit.getScheduler().runTask(plugin, () -> economy.withdrawPlayer(player, money));
            return true;
        }

        return false;
    }

    public boolean isVaultEnabled(){
        return isVaultEnabled;
    }

}
